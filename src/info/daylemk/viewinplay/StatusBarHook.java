
package info.daylemk.viewinplay;

import android.content.Context;
import android.content.res.Resources;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.lang.reflect.Method;

public class StatusBarHook {
    private static final String TAG = "DayL";
    private static final String TAG_CLASS = "[StatusBarHook]";

    private static int popupMenuId = 0;
    private static int inspectItemId = 0;

    public static void handleLoadPackage(final LoadPackageParam lpp, final XSharedPreferences pref) {
        if (!lpp.packageName.equals("com.android.systemui"))
            return;
        XposedBridge.log(TAG + TAG_CLASS + "handle package");
        pref.reload();

        if (pref.getBoolean(XposedInit.KEY_SHOW_IN_NOTIFICATION,
                Common.DEFAULT_SHOW_IN_NOTIFICATION)) {
            injectStatusBarMenu(lpp);
        }
    }

    private static void injectStatusBarMenu(final LoadPackageParam lpp) {
        XposedBridge.log(TAG + TAG_CLASS + "in inject status bar menu");
        final Class<?> hookClass = XposedHelpers.findClass(
                "com.android.systemui.statusbar.BaseStatusBar",
                lpp.classLoader);
        XposedBridge.hookAllMethods(hookClass, "getNotificationLongClicker", new XC_MethodHook() {
            protected void beforeHookedMethod(final MethodHookParam param) throws Throwable {
                final Object thiz = param.thisObject;
                final Context mContext = (Context) XposedHelpers.getObjectField(thiz, "mContext");
                Resources res = mContext.getResources();
                // if the id is not set or the popup menu id is invalid
                if (popupMenuId == 0) {
                    popupMenuId = res.getIdentifier("notification_popup_menu", "menu",
                            "com.android.systemui");
                }
                XposedBridge.log(TAG + TAG_CLASS + "the menu id is " + popupMenuId);
                if (inspectItemId == 0) {
                    inspectItemId = res.getIdentifier("notification_inspect_item", "id",
                            "com.android.systemui");
                }
                XposedBridge.log(TAG + TAG_CLASS + "the inspect menu id is " + inspectItemId);

                View.OnLongClickListener listerner = new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        final String packageNameF = (String) v.getTag();
                        if (packageNameF == null)
                            return false;
                        if (v.getWindowToken() == null)
                            return false;

                        PopupMenu mPopupMenu = new PopupMenu(mContext, v);
                        if (popupMenuId != 0) {
                            mPopupMenu.getMenuInflater().inflate(
                                    popupMenuId,
                                    mPopupMenu.getMenu());
                        } else {
                            XposedBridge.log(TAG + TAG_CLASS + "the popup menu id is 0 ???");
                            mPopupMenu.getMenu().add(Menu.NONE, RecentTaskHook.ID_APP_INFO, 1,
                                    RecentTaskHook.TEXT_APP_INFO);
                        }
                        // if it's the android stock app, let's do nothing
                        if (!RecentTaskHook.isAndroidStockApp(packageNameF)) {
                            mPopupMenu.getMenu().add(Menu.NONE, RecentTaskHook.ID_VIEW_IN_PLAY, 4,
                                    RecentTaskHook.TEXT_VIEW_IN_PLAY);
                            Common.debugLog(TAG + TAG_CLASS + "not stock app : " + packageNameF);
                        } else {
                            XposedBridge.log(TAG + TAG_CLASS + "stock app : " + packageNameF);
                        }

                        mPopupMenu
                                .setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                    public boolean onMenuItemClick(MenuItem item) {
                                        int itemId = item.getItemId();
                                        Common.debugLog(TAG + TAG_CLASS
                                                + "the item id clicked is : " + itemId);
                                        if (itemId == inspectItemId) {
                                            Common.debugLog(TAG + TAG_CLASS + "inspectItemId : "
                                                    + itemId);
                                            try {
                                                Method method = thiz
                                                        .getClass()
                                                        .getSuperclass()
                                                        .getDeclaredMethod(
                                                                "startApplicationDetailsActivity",
                                                                String.class);
                                                method.setAccessible(true);
                                                method.invoke(thiz, packageNameF);
                                            } catch (Exception e) {
                                                XposedBridge.log(e);
                                                // if we got exception here,
                                                // let's use my way
                                                RecentTaskHook.startApplicationDetailsActivity(
                                                        mContext, packageNameF);
                                            }
                                            collapsePanels(thiz);
                                            return true;
                                        } else if (itemId == RecentTaskHook.ID_VIEW_IN_PLAY) {
                                            Common.debugLog(TAG + TAG_CLASS + "view in play : "
                                                    + itemId);
                                            RecentTaskHook.viewInPlay(mContext, packageNameF);
                                            collapsePanels(thiz);
                                            return true;
                                        }
                                        XposedBridge.log(TAG + TAG_CLASS + "not handled click : "
                                                + itemId);
                                        return false;
                                    }
                                });
                        // don't forget to set the pupop menu back
                        XposedHelpers.setObjectField(thiz, "mNotificationBlamePopup", mPopupMenu);
                        mPopupMenu.show();
                        return true;
                    }
                };
                param.setResult(listerner);
            }
        });
    }

    /**
     * this should worked with 4.1 and newer
     * @param thiz
     */
    private static void collapsePanels(Object thiz) {
        try {
            // 4.2 and newer
            XposedHelpers.callMethod(
                    thiz,
                    "animateCollapsePanels",
                    // can't get it, wired
                    // :(
                    // CommandQueue.FLAG_EXCLUDE_NONE
                    0
                    );
        } catch (Exception e) {
            // 4.1
            XposedHelpers.callMethod(
                    thiz,
                    "animateCollapse",
                    // CommandQueue.FLAG_EXCLUDE_NONE
                    0
                    );
        }
    }
}

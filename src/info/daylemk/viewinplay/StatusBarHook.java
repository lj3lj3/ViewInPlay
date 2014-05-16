
package info.daylemk.viewinplay;

import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class StatusBarHook {
    private static final String TAG = "DayL";
    private static final String TAG_CLASS = "[StatusBarHook]";

    private static int popupMenuId = 0;
    private static int inspectItemId = 0;
    // added PA floating mode
    private static int floatingItemId = 0;
    private static Object statusBar;

    public static void handleLoadPackage(final LoadPackageParam lpp, final XSharedPreferences pref) {
        if (!lpp.packageName.equals("com.android.systemui"))
            return;
        Common.debugLog(TAG + TAG_CLASS + "handle package");
        pref.reload();

        injectStatusBarCollapse(lpp);

        if (pref.getBoolean(XposedInit.KEY_SHOW_IN_NOTIFICATION,
                Common.DEFAULT_SHOW_IN_NOTIFICATION)) {
            injectStatusBarMenu(lpp);
        }
    }

    private static void injectStatusBarCollapse(final LoadPackageParam lpp) {
        Common.debugLog(TAG + TAG_CLASS + "in inject status bar collapse");
        final Class<?> hookClass = XposedHelpers.findClass(
                "com.android.systemui.statusbar.BaseStatusBar",
                lpp.classLoader);
        XposedBridge.hookAllMethods(hookClass, "start", new XC_MethodHook() {

            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                if (statusBar != null) {
                    XposedBridge
                            .log(TAG + TAG_CLASS + "the status bar not be cleared last time???");
                }
                statusBar = param.thisObject;
                Common.debugLog(TAG + TAG_CLASS + "start status bar");
            }
        });

        XposedBridge.hookAllMethods(hookClass, "destroy", new XC_MethodHook() {

            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                statusBar = null;
                Common.debugLog(TAG + TAG_CLASS + "destory status bar");
            }
        });
    }

    private static void injectStatusBarMenu(final LoadPackageParam lpp) {
        Common.debugLog(TAG + TAG_CLASS + "in inject status bar menu");
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
                Common.debugLog(TAG + TAG_CLASS + "the menu id is " + popupMenuId);
                if (inspectItemId == 0) {
                    inspectItemId = res.getIdentifier("notification_inspect_item", "id",
                            "com.android.systemui");
                }
                Common.debugLog(TAG + TAG_CLASS + "the inspect menu id is " + inspectItemId);
                // added PA floating mode'
                // MOD always get the id
                if (floatingItemId == 0) {
                    floatingItemId = res.getIdentifier("notification_floating_item", "id",
                            "com.android.systemui");
                }
                Common.debugLog(TAG + TAG_CLASS + "the floating menu id is " + floatingItemId);

                View.OnLongClickListener listerner = new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        Object tag = v.getTag();
                        String pkgNameF = null;
                        PendingIntent contentIntentF = null;
                        if (tag instanceof String) {
                            pkgNameF = (String) v.getTag();
                        } else {
                            Common.debugLog(TAG + TAG_CLASS + "PA mode");
                            // PA new floating mode, NotificationData.Entry
                            try {
                                Field fieldNoti = tag.getClass().getDeclaredField("notification");
                                fieldNoti.setAccessible(true);
                                Object sbn = fieldNoti.get(tag);
                                
                                Method methodGPN = sbn.getClass().getDeclaredMethod("getPackageName");
                                methodGPN.setAccessible(true);
                                pkgNameF = (String) methodGPN.invoke(sbn);
                                
                                Method methodGNoti = sbn.getClass().getDeclaredMethod("getNotification"); 
                                methodGNoti.setAccessible(true);
                                Object notifi = methodGNoti.invoke(sbn);
                                
                                Field fieldCI = notifi.getClass().getDeclaredField("contentIntent");
                                fieldCI.setAccessible(true);
                                contentIntentF = (PendingIntent) fieldCI.get(notifi);
                            } catch (Exception e) {
                                e.printStackTrace();
                                Common.debugLog(e.toString());
                            }
                        }

                        final PendingIntent contentIntent = contentIntentF;
                        final String packageNameF = pkgNameF;

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
                            Common.debugLog(TAG + TAG_CLASS + "the popup menu id is 0 ???");
                            mPopupMenu.getMenu().add(Menu.NONE, RecentTaskHook.ID_APP_INFO, 1,
                                    RecentTaskHook.TEXT_APP_INFO);
                            // added PA floating mode, also available with stock
                            // APP
                            // TODO get the stock text here
                            if (XposedInit.bool_compat_floating) {
                                mPopupMenu.getMenu().add(Menu.NONE,
                                        RecentTaskHook.ID_LAUNCH_FLOATING, 5,
                                        RecentTaskHook.TEXT_LAUNCH_FLOATING);
                            }
                        }
                        // if it's the android stock app, let's do nothing
                        if (!RecentTaskHook.isAndroidStockApp(packageNameF)) {
                            mPopupMenu.getMenu().add(Menu.NONE, RecentTaskHook.ID_VIEW_IN_PLAY, 4,
                                    RecentTaskHook.TEXT_VIEW_IN_PLAY);
                            Common.debugLog(TAG + TAG_CLASS + "not stock app : " + packageNameF);
                        } else {
                            Common.debugLog(TAG + TAG_CLASS + "stock app : " + packageNameF);
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
                                        } else if (itemId == floatingItemId) {
                                            if (contentIntent == null) {
                                                Common.debugLog(TAG + TAG_CLASS
                                                        + "the content intent is null??? ");
                                                return false;
                                            }
                                            try {
                                                Method method = thiz.getClass().getSuperclass()
                                                        .getDeclaredMethod("launchFloating",
                                                                PendingIntent.class);
                                                method.setAccessible(true);
                                                method.invoke(thiz, contentIntent);
                                            } catch (IllegalAccessException e) {
                                                e.printStackTrace();
                                            } catch (IllegalArgumentException e) {
                                                e.printStackTrace();
                                            } catch (InvocationTargetException e) {
                                                e.printStackTrace();
                                            } catch (NoSuchMethodException e) {
                                                e.printStackTrace();
                                            }
                                            collapsePanels(thiz);
                                        }
                                        Common.debugLog(TAG + TAG_CLASS + "not handled click : "
                                                + itemId);
                                        return false;
                                    }
                                });
                        // don't forget to set the popup menu back
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
     * 
     * @param thiz
     */
    private static void collapsePanels(Object thiz) {
        Method collapsePanel;
        // 4.2 and newer
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            Common.debugLog(TAG + TAG_CLASS + "thiz : " + thiz + ", > 4.1");
            try {
                // should fix SystemUI crash on < 4.2 version
                // 'cause can't catch any exception here
                collapsePanel = thiz.getClass().getDeclaredMethod("animateCollapsePanels");
                collapsePanel.setAccessible(true);
                collapsePanel.invoke(thiz);
            } catch (Exception e) {
                XposedBridge.log(e);
            }
        } else {
            // 4.1
            Common.debugLog(TAG + TAG_CLASS + "animate collapse 4.1");
            try {
                collapsePanel = thiz.getClass()
                        .getDeclaredMethod("animateCollapse");
                collapsePanel.setAccessible(true);
                collapsePanel.invoke(thiz);
            } catch (Exception e1) {
                XposedBridge.log(e1);
            }
        }
    }

    /**
     * collapse the status bar
     * 
     * @return
     */
    public static boolean collapseStatusBarPanel() {
        if (statusBar == null) {
            Common.debugLog(TAG + TAG_CLASS + "the status bar is null");
            return false;
        }

        collapsePanels(statusBar);
        return true;
    }
}

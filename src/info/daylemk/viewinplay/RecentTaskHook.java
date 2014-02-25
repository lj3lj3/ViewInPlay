
package info.daylemk.viewinplay;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.res.XModuleResources;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class RecentTaskHook {
    private static final String TAG = "DayL";
    private static final String TAG_CLASS = "[RecentTaskHook]";

    static String TEXT_APP_INFO;
    // static String TEXT_OPEN_IN_HALO;
    static String TEXT_REMOVE_FROM_LIST;
    static String TEXT_VIEW_IN_PLAY;

    static final int ID_REMOVE_FROM_LIST = 1000;
    static final int ID_APP_INFO = 2000;
    // static final int ID_OPEN_IN_HALO = 3000;
    static final int ID_VIEW_IN_PLAY = 4000;

    public static void initZygote(XModuleResources module_res) {
        TEXT_APP_INFO = module_res.getString(R.string.recents_app_info);
        // TEXT_OPEN_IN_HALO = module_res.getString(R.string.recents_open_halo);
        TEXT_REMOVE_FROM_LIST = module_res.getString(R.string.recents_remove_from_list);
        TEXT_VIEW_IN_PLAY = module_res.getString(R.string.view_in_play);
    }

    public static void handleLoadPackage(final LoadPackageParam lpp, final XSharedPreferences pref) {
        if (!lpp.packageName.equals("com.android.systemui"))
            return;
        XposedBridge.log(TAG + TAG_CLASS + "handle package");
        XposedBridge.log(TAG + TAG_CLASS + "XposedInit.KEY_SHOW_IN_RECENT_PANEL = "
                + XposedInit.KEY_SHOW_IN_RECENT_PANEL);

        StringBuffer sb = new StringBuffer();
        Set<String> set = pref.getAll().keySet();
        XposedBridge.log(TAG + TAG_CLASS + "size : " + set.size());
        for (Iterator<String> iterator = set.iterator(); iterator.hasNext();) {
            sb.append(iterator.next() + ", ");
        }
        XposedBridge.log(TAG + TAG_CLASS + "keys : " + sb.toString());
        XposedBridge.log(TAG + TAG_CLASS + "contain???"
                + pref.contains(XposedInit.KEY_SHOW_IN_RECENT_PANEL));

        pref.reload();
        if (pref.getBoolean(XposedInit.KEY_SHOW_IN_RECENT_PANEL,
                Common.DEFAULT_SHOW_IN_RECENT_PANEL)) {
            injectMenu(lpp);
        }
    }

    private static void injectMenu(final LoadPackageParam lpp) {
        XposedBridge.log(TAG + TAG_CLASS + "in inject menu");
        final Class<?> hookClass = XposedHelpers.findClass(
                "com.android.systemui.recent.RecentsPanelView",
                lpp.classLoader);
        XposedBridge.hookAllMethods(hookClass, "handleLongPress", new XC_MethodHook() {
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                final View thiz = (View) param.thisObject;
                final View selectedView = (View) param.args[0];
                final View anchorView = (View) param.args[1];
                final View thumbnailView = (View) param.args[2];

                thumbnailView.setSelected(true);
                final Object viewHolder = selectedView.getTag();

                // if the app is stock app, we should not show the 'view in
                // play' menu
                String pkgName = getPackageName(viewHolder);
                Log.d(TAG, TAG_CLASS + "the package is : " + pkgName);
                if (isAndroidStockApp(pkgName)) {
                    // stock app, return
                    return;
                }

                PopupMenu popup = new PopupMenu(thiz.getContext(),
                        anchorView == null ? selectedView : anchorView);
                popup.getMenu().add(Menu.NONE, ID_REMOVE_FROM_LIST, 1, TEXT_REMOVE_FROM_LIST);
                popup.getMenu().add(Menu.NONE, ID_APP_INFO, 2, TEXT_APP_INFO);
                // popup.getMenu().add(Menu.NONE, ID_OPEN_IN_HALO, 3,
                // TEXT_OPEN_IN_HALO);
                Log.d(TAG, TAG_CLASS + "show the vip menu : " + pkgName);
                popup.getMenu().add(Menu.NONE, ID_VIEW_IN_PLAY, 4, TEXT_VIEW_IN_PLAY);

                try {
                    thiz.getClass().getDeclaredField("mPopup").set(thiz, popup);
                } catch (Exception e) {
                    // User on ICS
                }

                final PopupMenu.OnMenuItemClickListener menu = new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        try {
                            switch (item.getItemId()) {
                                case ID_REMOVE_FROM_LIST:
                                    ViewGroup recentsContainer = (ViewGroup) hookClass
                                            .getDeclaredField("mRecentsContainer").get(thiz);
                                    recentsContainer.removeViewInLayout(selectedView);
                                    return true;
                                case ID_APP_INFO:
                                    if (viewHolder != null) {
                                        closeRecentApps(thiz);
                                        // Object ad = viewHolder.getClass()
                                        // .getDeclaredField("taskDescription")
                                        // .get(viewHolder);
                                        // String pkg_name = (String)
                                        // ad.getClass()
                                        // .getDeclaredField("packageName").get(ad);
                                        startApplicationDetailsActivity(thiz.getContext(),
                                                getPackageName(viewHolder));
                                    }
                                    return true;
                                    // case ID_OPEN_IN_HALO:
                                    // if (viewHolder != null) {
                                    // closeRecentApps(thiz);
                                    // Object ad = viewHolder.getClass()
                                    // .getDeclaredField("taskDescription")
                                    // .get(viewHolder);
                                    // Intent intent = (Intent) ad.getClass()
                                    // .getDeclaredField("intent").get(ad);
                                    // intent.addFlags(Common.FLAG_FLOATING_WINDOW
                                    // | Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                                    // | Intent.FLAG_ACTIVITY_NO_USER_ACTION
                                    // | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    // thiz.getContext().startActivity(intent);
                                    // }
                                    // return true;
                                case ID_VIEW_IN_PLAY:
                                    if (viewHolder != null) {
                                        closeRecentApps(thiz);
                                        // Object ad = viewHolder.getClass()
                                        // .getDeclaredField("taskDescription")
                                        // .get(viewHolder);
                                        // String pkg_name = (String)
                                        // ad.getClass()
                                        // .getDeclaredField("packageName").get(ad);
                                        viewInPlay(thiz.getContext(), getPackageName(viewHolder));
                                    }
                                    return true;
                            }
                        } catch (Throwable t) {
                            XposedBridge.log(Common.LOG_TAG + "RecentAppsHook / onMenuItemClick ("
                                    + item.getItemId() + ")");
                            XposedBridge.log(t);
                        }
                        return false;
                    }
                };
                popup.setOnMenuItemClickListener(menu);
                popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
                    public void onDismiss(PopupMenu menu) {
                        thumbnailView.setSelected(false);
                        try {
                            thiz.getClass().getDeclaredField("mPopup").set(thiz, null);
                        } catch (Exception e) {
                            // User on ICS
                        }
                    }
                });
                popup.show();
                param.setResult(null);
            }
        });
    }

    private static void closeRecentApps(View thiz) {
        if (Build.VERSION.SDK_INT >= 16) {
            try {
                XposedHelpers.callMethod(thiz, "dismissAndGoBack");
                return;
            } catch (Exception e) {
            }
        }

        // those is for the sdk version lower than 16
        /*
         * StatusBarManager bar = (StatusBarManager)
         * thiz.getContext().getSystemService("statusbar"); if (bar != null) {
         * try { bar.collapse(); return; } catch (Throwable e) { } }
         */

        new Thread() {
            @Override
            public void run() {
                try {
                    Runtime.getRuntime().exec("input keyevent " + KeyEvent.KEYCODE_BACK);
                } catch (Exception e) {
                }
            }
        }.start();
    }

    private static void startApplicationDetailsActivity(Context ctx, String packageName) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts(
                "package", packageName, null));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ctx.startActivity(intent);
    }

    /**
     * get the package name from the view holder object
     * 
     * @param viewHolder
     * @return
     * @throws Throwable
     */
    private static String getPackageName(final Object viewHolder) throws Throwable {
        String pkg_name = "";
        if (viewHolder != null) {
            Object ad = viewHolder.getClass()
                    .getDeclaredField("taskDescription")
                    .get(viewHolder);
            pkg_name = (String) ad.getClass()
                    .getDeclaredField("packageName").get(ad);
        }
        return pkg_name;
    }

    /**
     * check if the package is the stock app or not
     * 
     * @param pkgName
     * @return
     */
    static boolean isAndroidStockApp(String pkgName) {
        if (pkgName.startsWith("com.android"))
            return true;
        return false;
    }

    /**
     * view the package in play, use the default access permission, so we can
     * call this method in this package
     * 
     * @param ctx
     * @param packageName
     */
    static void viewInPlay(Context ctx, String packageName) {
        XposedBridge.log(TAG + TAG_CLASS + "view in play : " + packageName);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("http://play.google.com/store/apps/details?id=" + packageName));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        List<ResolveInfo> list = ctx.getPackageManager().queryIntentActivities(intent, 0);
        if (XposedInit.directlyShowInPlay) {
            String pkgName;
            for (int i = 0; i < list.size(); i++) {
                ActivityInfo info = list.get(i).activityInfo;
                XposedBridge.log(TAG + TAG_CLASS + "the package name is " + info.packageName);
                pkgName = info.packageName;
                if (pkgName.equals("com.android.vending")) {
                    XposedBridge.log(TAG + TAG_CLASS + "we found it : " + pkgName);
                    String appInfo = info.toString();
                    XposedBridge.log(TAG + TAG_CLASS + "app info string : " + appInfo);
                    String activityName = appInfo.substring(
                            appInfo.lastIndexOf(" ") + 1, appInfo.length() - 1);
                    XposedBridge.log(TAG + TAG_CLASS + "activity name : " + activityName);
                    intent.setComponent(new ComponentName(pkgName, activityName));
                    break;
                }
            }
            XposedBridge.log(TAG + TAG_CLASS + "user wants to show [" + packageName + "] in the play, but no play here, pity");
            // if didn't find the Google Play Store, show the toast
            Toast.makeText(ctx, R.string.no_play_on_the_phone, Toast.LENGTH_LONG).show();
        }
        ctx.startActivity(intent);
    }
}


package info.daylemk.viewinplay;

import android.app.Fragment;
import android.content.pm.ApplicationInfo;
import android.content.res.XModuleResources;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class AppInfoHook {
    private static final String TAG = "DayL";
    private static final String TAG_CLASS = "[AppInfoHook]";

    public static void initZygote(XModuleResources module_res) {
        // TEXT_APP_INFO = module_res.get
        // // TEXT_OPEN_IN_HALO =
        // module_res.getString(R.string.recents_open_halo);
        // TEXT_REMOVE_FROM_LIST =
        // module_res.getString(R.string.recents_remove_from_list);
        // TEXT_VIEW_IN_PLAY = module_res.getString(R.string.view_in_play);
    }

    public static void handleLoadPackage(final LoadPackageParam lpp, final XSharedPreferences pref) {
        if (!lpp.packageName.equals("com.android.settings"))
            return;
        XposedBridge.log(TAG + TAG_CLASS + "handle load package");
        Common.debugLog(TAG + TAG_CLASS + "directlyShowInPlay = "
                + XposedInit.directlyShowInPlay);
        XposedBridge.log(TAG + TAG_CLASS + "contain???"
                + pref.contains(XposedInit.KEY_SHOW_IN_APP_INFO));
        pref.reload();
        if (pref.getBoolean(XposedInit.KEY_SHOW_IN_APP_INFO,
                Common.DEFAULT_SHOW_IN_APP_INFO)) {
            injectMenu(lpp);
        }
    }

    public static void handleInitPackageResources(InitPackageResourcesParam resparam) {
        if (!resparam.packageName.equals("com.android.settings"))
            return;
        XposedBridge.log(TAG + TAG_CLASS + "handle init resouces");
        resparam.res.hookLayout("com.android.settings", "layout", "installed_app_details",
                new XC_LayoutInflated() {
                    @Override
                    public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                        LinearLayout view = (LinearLayout) liparam.view.findViewById(liparam.res
                                .getIdentifier("all_details", "id",
                                        "com.android.settings"));
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);

                        Button viewInPlayButton = new Button(view.getContext());
                        viewInPlayButton.setId(R.id.id_view_in_play_app_info);
                        viewInPlayButton.setText(RecentTaskHook.TEXT_VIEW_IN_PLAY);
                        viewInPlayButton.setVisibility(View.GONE);
                        // viewInPlayButton.setOnClickListener(new
                        // View.OnClickListener() {
                        // @Override
                        // public void onClick(View v) {
                        // }
                        // });
                        view.addView(viewInPlayButton, 0, params);
                    }
                });
    }

    private static void injectMenu(final LoadPackageParam lpp) {
        XposedBridge.log(TAG + TAG_CLASS + "in inject menu");
        final Class<?> hookClass = XposedHelpers.findClass(
                "com.android.settings.applications.InstalledAppDetails",
                lpp.classLoader);
        XposedBridge.hookAllMethods(hookClass, "onCreateView", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                final Fragment thiz = (Fragment) param.thisObject;
                Common.debugLog(TAG + TAG_CLASS + "thiz : " + thiz);

                Object appEntry = XposedHelpers.getObjectField(param.thisObject, "mAppEntry");
                Common.debugLog(TAG + TAG_CLASS + "we found the mAppEntry : " + appEntry);
                final ApplicationInfo info = (ApplicationInfo) XposedHelpers.getObjectField(
                        appEntry,
                        "info");
                Common.debugLog(TAG + TAG_CLASS + "we found the app info : " + info);
                XposedBridge.log(TAG + TAG_CLASS + "the package name is : " + info.packageName);

                if (!RecentTaskHook.isAndroidStockApp(info.packageName)) {
                    final View rootView = (View) XposedHelpers.getObjectField(param.thisObject,
                            "mRootView");
                    Button viewInPlayButton = (Button) rootView
                            .findViewById(R.id.id_view_in_play_app_info);
                    viewInPlayButton.setVisibility(View.VISIBLE);
                    viewInPlayButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            RecentTaskHook.viewInPlay(thiz.getActivity(), info.packageName);
                        }
                    });
                } else {
                    XposedBridge.log(TAG + TAG_CLASS + "stock app : " + info.packageName);
                }
            }
        });
    }
}

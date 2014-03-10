
package info.daylemk.viewinplay;

import android.content.res.XModuleResources;
import android.widget.TextView;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources.InitPackageResourcesParam;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class XposedInit implements IXposedHookLoadPackage, IXposedHookZygoteInit,
        IXposedHookInitPackageResources {
    private static final String TAG = "DayL";

    public static XModuleResources sModRes;

    static String KEY_DIRECTLY_SHOW_IN_PLAY;
    static String KEY_SHOW_IN_APP_INFO;
    static String KEY_SHOW_IN_RECENT_PANEL;
    static String KEY_DOUBLE_IN_RECENT_PANEL;
    static boolean directlyShowInPlay = false;

    private static List<String> notStockApp;
    private static List<String> stockAndroidApp;
    private static String MODULE_PATH = null;
    private static XSharedPreferences mPref;
    private static TextView tvClock;

    public static TextView getClock() {
        return tvClock;
    }

    @Override
    public void initZygote(StartupParam startupParam) throws Throwable {
        mPref = new XSharedPreferences(Common.THIS_PACKAGE_NAME, Common.PREFERENCE_MAIN_FILE);
        MODULE_PATH = startupParam.modulePath;
        sModRes = XModuleResources.createInstance(MODULE_PATH, null);
        RecentTaskHook.initZygote(sModRes);
        AppInfoHook.initZygote(sModRes);

        KEY_DIRECTLY_SHOW_IN_PLAY = sModRes.getString(R.string.key_directly_show_in_play);
        KEY_SHOW_IN_RECENT_PANEL = sModRes.getString(R.string.key_show_in_recent_panel);
        KEY_SHOW_IN_APP_INFO = sModRes.getString(R.string.key_show_in_app_info);
        KEY_DOUBLE_IN_RECENT_PANEL = sModRes.getString(R.string.key_double_in_recent_panel);
        notStockApp = Arrays.asList(sModRes.getStringArray(R.array.not_stock_app));
        stockAndroidApp = Arrays.asList(sModRes.getStringArray(R.array.stock_android_app));
    }

    @Override
    public void handleLoadPackage(final LoadPackageParam lpparam)
            throws Throwable {
        loadPref(lpparam);

        RecentTaskHook.handleLoadPackage(lpparam, mPref);
        AppInfoHook.handleLoadPackage(lpparam, mPref);
    }

    private void loadPref(final LoadPackageParam lpparam) {
        if (!lpparam.packageName.equals("com.android.systemui")
                // FIXED the directly view in play not effect in the app info
                // screen
                && !lpparam.packageName.equals("com.android.settings"))
            return;

        XposedBridge.log(TAG + "lpparam.packageName:" + lpparam.packageName);

        mPref.reload();
        directlyShowInPlay = mPref.getBoolean(KEY_DIRECTLY_SHOW_IN_PLAY,
                Common.DEFAULT_DIRECTLY_SHOW_IN_PLAY);

        XposedBridge.log(TAG + "the directly is " + directlyShowInPlay);
    }

    @Override
    public void handleInitPackageResources(InitPackageResourcesParam resparam) throws Throwable {
        AppInfoHook.handleInitPackageResources(resparam);
    }

    public static boolean isStockAndroidApp(String pkgName) {
        if (stockAndroidApp.contains(pkgName))
            return true;
        return false;
    }

    public static boolean isNotStockApp(String pkgName) {
        if (notStockApp.contains(pkgName))
            return true;
        return false;
    }
}

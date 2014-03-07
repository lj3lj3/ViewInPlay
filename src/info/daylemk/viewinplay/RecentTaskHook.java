
package info.daylemk.viewinplay;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.XModuleResources;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
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
    static String TEXT_NO_PLAY;

    static final int ID_REMOVE_FROM_LIST = 1000;
    static final int ID_APP_INFO = 2000;
    // static final int ID_OPEN_IN_HALO = 3000;
    static final int ID_VIEW_IN_PLAY = 4000;
    
    static int id_app_thumbnail = -1;
    
    static GestureDetector mGestureDetector;
    static GestureListener mGestureListener;

    public static void initZygote(XModuleResources module_res) {
        TEXT_APP_INFO = module_res.getString(R.string.recents_app_info);
        // TEXT_OPEN_IN_HALO = module_res.getString(R.string.recents_open_halo);
        TEXT_REMOVE_FROM_LIST = module_res.getString(R.string.recents_remove_from_list);
        TEXT_VIEW_IN_PLAY = module_res.getString(R.string.view_in_play);
        TEXT_NO_PLAY = module_res.getString(R.string.no_play_on_the_phone);
    }

    public static void handleLoadPackage(final LoadPackageParam lpp, final XSharedPreferences pref) {
        if (!lpp.packageName.equals("com.android.systemui"))
            return;
        XposedBridge.log(TAG + TAG_CLASS + "handle package");
        Common.debugLog(TAG + TAG_CLASS + "directlyShowInPlay = "
                + XposedInit.directlyShowInPlay);

        StringBuffer sb = new StringBuffer();
        Set<String> set = pref.getAll().keySet();
        XposedBridge.log(TAG + TAG_CLASS + "size : " + set.size());
        for (Iterator<String> iterator = set.iterator(); iterator.hasNext();) {
            sb.append(iterator.next() + ", ");
        }
        Common.debugLog(TAG + TAG_CLASS + "keys : " + sb.toString());
        XposedBridge.log(TAG + TAG_CLASS + "contain???"
                + pref.contains(XposedInit.KEY_SHOW_IN_RECENT_PANEL));

        pref.reload();
        if (pref.getBoolean(XposedInit.KEY_SHOW_IN_RECENT_PANEL,
                Common.DEFAULT_SHOW_IN_RECENT_PANEL)) {
            injectMenu(lpp);
        }
        
        injectTouchEvents(lpp);
 
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
                XposedBridge.log(TAG + TAG_CLASS + "the package is : " + pkgName);
                if (isAndroidStockApp(pkgName)) {
                    // stock app, return
                    XposedBridge.log(TAG + TAG_CLASS + "stock app");
                    return;
                }

                PopupMenu popup = new PopupMenu(thiz.getContext(),
                        anchorView == null ? selectedView : anchorView);
                popup.getMenu().add(Menu.NONE, ID_REMOVE_FROM_LIST, 1, TEXT_REMOVE_FROM_LIST);
                popup.getMenu().add(Menu.NONE, ID_APP_INFO, 2, TEXT_APP_INFO);
                // popup.getMenu().add(Menu.NONE, ID_OPEN_IN_HALO, 3,
                // TEXT_OPEN_IN_HALO);
                Common.debugLog(TAG + TAG_CLASS + "show the vip menu : " + pkgName);
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
    
    private static void injectTouchEvents(final LoadPackageParam lpp) {
        XposedBridge.log(TAG + TAG_CLASS + "in inject touch eventss");
        final Class<?> hookClass = XposedHelpers.findClass(
                "com.android.systemui.recent.RecentsVerticalScrollView",
                lpp.classLoader);
        XposedBridge.hookAllMethods(hookClass, "update", new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                
                /*
//              final Class<?> rClass = XposedHelpers.findClass("com.android.systemui.R.id", lpp.classLoader);
              try {
                  Class.forName("com.android.systemui.R.id", false, lpp.classLoader);
              } catch (ClassNotFoundException e1) {
                  e1.printStackTrace();
                  XposedBridge.log(TAG + TAG_CLASS + "ERROR:" + "ClassNotFoundException id");
              }
              try {
                  Class.forName("com.android.systemui.R", false, lpp.classLoader);
              } catch (ClassNotFoundException e1) {
                  e1.printStackTrace();
                  XposedBridge.log(TAG + TAG_CLASS + "ERROR:" + "ClassNotFoundException R");
              }
              final Class<?> rClass = XposedHelpers.findClass("com.android.systemui.R", lpp.classLoader);
              int id_app_thumbnail = 0;
              try {
                  Class<?>[] classes = rClass.getDeclaredClasses();
                  XposedBridge.log(TAG + TAG_CLASS + "length :" + classes.length);
                  
                  XposedBridge.log(TAG + TAG_CLASS + "length of classes:" + rClass.getClasses().length);
                  for (int k = 0; k < classes.length; k ++){
                      XposedBridge.log(TAG + TAG_CLASS + "name:" + classes[k].getName());
                      if(classes[k].getName() == "com.android.systemui.R$id"){
                          id_app_thumbnail = classes[k].getDeclaredField("app_thumbnail").getInt(null);
                          XposedBridge.log(TAG + TAG_CLASS + "get int :" + id_app_thumbnail);
                      }
                  }
              } catch (NoSuchFieldException e) {
                  // TODO Auto-generated catch block
                  XposedBridge.log(TAG + TAG_CLASS + "ERROR:" + "NoSuchFieldException");
                  e.printStackTrace();
              } catch (IllegalAccessException e) {
                  // TODO Auto-generated catch block
                  XposedBridge.log(TAG + TAG_CLASS + "ERROR:" + "IllegalAccessException");
                  e.printStackTrace();
              } catch (IllegalArgumentException e) {
                  // TODO Auto-generated catch block
                  XposedBridge.log(TAG + TAG_CLASS + "ERROR:" + "IllegalArgumentException");
                  e.printStackTrace();
              }
              
              XposedBridge.log(TAG + TAG_CLASS + "GOTIT:" + "id_ap_thumbnail:" + id_app_thumbnail);
              
              if(id_app_thumbnail != 0){
                  
              } else {
                  
              }*/
                
                final Object thiz = param.thisObject;
                final ScrollView sv = (ScrollView)thiz;
                Resources res = sv.getContext().getResources();
                
                
                if(id_app_thumbnail == -1){
                    id_app_thumbnail = res.getIdentifier("app_thumbnail", "id", "com.android.systemui");
                    XposedBridge.log(TAG + TAG_CLASS + "identifier 2: " + id_app_thumbnail);
                }
                
                if(mGestureDetector == null){
                    mGestureListener = new GestureListener();
                    mGestureDetector = new GestureDetector(sv.getContext(), mGestureListener);
                }
                
                LinearLayout mLinearLayout = null;
                try {
                    mLinearLayout = (LinearLayout) thiz.getClass().getDeclaredField("mLinearLayout").get(thiz);
                } catch (Exception e) {
                    // User on ICS
                }
                if(mLinearLayout != null){
                    XposedBridge.log(TAG + TAG_CLASS + "in inject touch events, OK");
                    RecentsOnTouchLis touchListener = new RecentsOnTouchLis();
                    FrameLayout frameLayout;
                    for (int i = 0; i < mLinearLayout.getChildCount(); i ++){
                        XposedBridge.log(TAG + TAG_CLASS + "i = " + i);
                        frameLayout = (FrameLayout) mLinearLayout.getChildAt(i);
                        View vTemp = frameLayout.findViewById(id_app_thumbnail);
                        if(vTemp != null){
                            vTemp.setOnTouchListener(touchListener);
//                            vTemp.setOnClickListener(new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                Toast.makeText(sv.getContext(), "text", Toast.LENGTH_SHORT).show();
//                            }
//                        });
                        }
                        XposedBridge.log(TAG + TAG_CLASS + "the view we got is : " + vTemp);
                        /*frameLayout.setOnClickListener(null);
                        View RelativeView = frameLayout.getChildAt(0);
                        XposedBridge.log(TAG + TAG_CLASS + "the layout inside is " + RelativeView);
                        ViewGroup layout = (ViewGroup)RelativeView;
                        for (int j = 0; j < layout.getChildCount(); j ++){
                            View frameView = layout.getChildAt(j);
                            XposedBridge.log(TAG + TAG_CLASS + "the count : " + j + ", view is" + frameView);
                            if(frameView instanceof ViewGroup){
                                XposedBridge.log(TAG + TAG_CLASS + "the layout inside of inside is " + frameView);
                                ViewGroup layoutGroup = (ViewGroup)frameView;
                                XposedBridge.log(TAG + TAG_CLASS + "the layout inside of inside of inside is " + layoutGroup);
//                                layoutGroup.setOnClickListener(null);
                            }
                        }*/
                    }
                } else {
                    XposedBridge.log(TAG + TAG_CLASS + "in inject touch events, can't get linearLayout");
                }
            }
        });
    }
    
    private static class RecentsOnTouchLis implements View.OnTouchListener{

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            XposedBridge.log(TAG + TAG_CLASS + "on touch, v : " + v + ", events : " + event);
            XposedBridge.log(TAG + TAG_CLASS + "events, action : " + event.getAction());
            XposedBridge.log(TAG + TAG_CLASS + "events, pointer count : " + event.getPointerCount());
            mGestureListener.setView(v);
            if(mGestureDetector.onTouchEvent(event)){
                XposedBridge.log(TAG + TAG_CLASS + "EVENT has been recoganized ");
//                return true;
            }
            
            return true;
        }
    }
    
    private static class GestureListener extends GestureDetector.SimpleOnGestureListener {
        private View v;
        
        public void setView (View v){
            this.v = v;
        }
        
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            super.onDoubleTap(e);
            XposedBridge.log(TAG + TAG_CLASS + "events, DoubleTap : " + e);
            return true;
        }
        
        @Override
        public boolean onDoubleTapEvent(MotionEvent e) {
            super.onDoubleTapEvent(e);
            XposedBridge.log(TAG + TAG_CLASS + "events, DoubleTap ATE : " + e);
            return true;
        }

        /*@Override
        public boolean onDown(MotionEvent e) {
            super.onDown(e);
            XposedBridge.log(TAG + TAG_CLASS + "events, Down ATE : " + e);
            return true; 
        }*/

        /*@Override
        public boolean onSingleTapUp(MotionEvent e) {
            super.onSingleTapUp(e);
            XposedBridge.log(TAG + TAG_CLASS + "events, Single tap up, ATE : " + e);
            return true;
        }*/

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            super.onSingleTapConfirmed(e);
            v.performClick();
            XposedBridge.log(TAG + TAG_CLASS + "events, Single tap up confirmed. let it go : " + e);
            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {
            // TODO Auto-generated method stub
            super.onShowPress(e);
            v.requestFocus();
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
            v.performLongClick();
            XposedBridge.log(TAG + TAG_CLASS + "events, Long press : " + e);
        }
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
        /*StatusBarManager bar = (StatusBarManager)
                thiz.getContext().getSystemService("statusbar");
        if (bar != null) {
            try {
                bar.collapse();
                return;
            } catch (Throwable e) {
            }
        }*/

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
        if (XposedInit.isStockAndroidApp(pkgName))
            return true;
        if (XposedInit.isNotStockApp(pkgName))
            return false;
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
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_TASK_ON_HOME);

        checkPlay(ctx, intent);

        ctx.startActivity(intent);
    }

    private static void checkPlay(Context ctx, Intent intent) {
        List<ResolveInfo> list = ctx.getPackageManager().queryIntentActivities(intent, 0);
        XposedBridge.log(TAG + TAG_CLASS + "directlyShowInPlay:" + XposedInit.directlyShowInPlay);
        if (XposedInit.directlyShowInPlay) {
            String pkgName;
            Common.debugLog(TAG + TAG_CLASS + "size:" + list.size());
            for (int i = 0; i < list.size(); i++) {
                ActivityInfo info = list.get(i).activityInfo;
                Common.debugLog(TAG + TAG_CLASS + "the package name is " + info.packageName);
                pkgName = info.packageName;
                if (pkgName.equals("com.android.vending")) {
                    XposedBridge.log(TAG + TAG_CLASS + "we found it : " + pkgName);
                    String appInfo = info.toString();
                    Common.debugLog(TAG + TAG_CLASS + "app info string : " + appInfo);
                    String activityName = appInfo.substring(
                            appInfo.lastIndexOf(" ") + 1, appInfo.length() - 1);
                    Common.debugLog(TAG + TAG_CLASS + "activity name : " + activityName);
                    intent.setComponent(new ComponentName(pkgName, activityName));
                    return;
                }
            }
            XposedBridge.log(TAG + TAG_CLASS + "no play here, pity");
            // if didn't find the Google Play Store, show the toast
            // TODO Toast may can not show by system, so we should do later
            // about it, maybe a broadcast instead
            // DONE just need to get the resource at init moment
            Toast.makeText(ctx, TEXT_NO_PLAY,
                    Toast.LENGTH_LONG).show();
        }
    }
}

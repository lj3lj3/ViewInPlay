
package info.daylemk.viewinplay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

public class SettingsFragment extends PreferenceFragment implements
        OnSharedPreferenceChangeListener {
    private static final String TAG = "DayL";
//    private static final String TAG_CLASS = "[SettingsFragment]";

    public static final String TAG_OF_FRAGMENT = "SettingsFragment";

    private Activity mActivity;

    public static SettingsFragment getInstance(Bundle args) {
        SettingsFragment fragment = new SettingsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public SettingsFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = this.getActivity();
        // set the preference file name, so we can get it more convenience
        this.getPreferenceManager().setSharedPreferencesName(Common.PREFERENCE_MAIN_FILE);
        // set the mode of preference file to world readable, so we can get the
        // settings from the other process
        // TODO use broadcast instead?
        this.getPreferenceManager()
                .setSharedPreferencesMode(PreferenceActivity.MODE_WORLD_READABLE);
        addPreferencesFromResource(R.xml.pref_settings);

        hideIconPref();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // This could also be a ScrollView
        ListView list = (ListView) view.findViewById(android.R.id.list);
        // This could also be set in your layout, allows the list items to
        // scroll through the bottom padded area (navigation bar)
        list.setClipToPadding(false);
        // Sets the padding to the insets (include action bar and navigation bar
        // padding for the current device and orientation)
        // setInsets(this.getActivity(), list);
    }

    private void hideIconPref() {
        String comName = mActivity.getComponentName().getClassName();
        boolean hideIcon = this.getPreferenceManager().getSharedPreferences()
                .getBoolean(this.getResources().getString(R.string.key_hide_icon), false);
        boolean isAliasCom = comName.contains("Alias") ? true
                : false;
        Log.d(TAG, "hideIcon : " + hideIcon + ", isAliasCom : " + isAliasCom);
        CheckBoxPreference checkBoxPreference = (CheckBoxPreference) findPreference(this
                .getResources().getString(R.string.key_hide_icon));

        // if we don't come from the launcher, don't enable it
        if (!isAliasCom) {
            Log.d(TAG, "no alias com : " + comName);
            checkBoxPreference.setEnabled(false);
            if (!hideIcon) {
                checkBoxPreference.setSummaryOff(R.string.pref_summary_hide_icon_not_from_launcher);
            }
            // else {... 
            // this will set by system
        } else {
            // if we came from launcher, means the icon did not hide, do no check it
            checkBoxPreference.setChecked(false);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
            final Preference preference) {
        int titleRes = preference.getTitleRes();
        if (titleRes == R.string.pref_title_xda) {
            Log.d(TAG, "view the xda thread");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("http://forum.xda-developers.com/showthread.php?p=50636629"));
            this.getActivity().startActivity(intent);
        } else if (titleRes == R.string.pref_title_hide_icon) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(R.string.notice_hide_icon)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            preference.setEnabled(false);

                            Log.d(TAG, "com : " + mActivity.getComponentName());
                            // only if the component name contain alias
                            if (mActivity.getComponentName().getClassName().contains("Alias")) {
                                mActivity.getPackageManager()
                                        .setComponentEnabledSetting(mActivity.getComponentName(),
                                                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                                                PackageManager.DONT_KILL_APP);
                            } else {
                                Log.d(TAG,
                                        "do not action, the icon already hid, this should NOT usually happen");
                            }
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    CheckBoxPreference checkPreference = (CheckBoxPreference) preference;
                                    checkPreference.setChecked(false);
                                }
                            }).setCancelable(false)
                    .create().show();
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.getPreferenceManager().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        this.getPreferenceManager().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
        super.onResume();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // nothing now
    }

    /*
     * public void setInsets(Activity context, View view) { if
     * (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) return;
     * SystemBarTintManager tintManager = new SystemBarTintManager(context);
     * SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
     * view.setPadding(0, config.getPixelInsetTop(true),
     * config.getPixelInsetRight(), config.getPixelInsetBottom()); }
     */
}

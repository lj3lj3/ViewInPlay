
package info.daylemk.viewinplay;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.readystatesoftware.systembartint.SystemBarTintManager;

public class SettingsFragment extends PreferenceFragment {
    private static final String TAG = "DayL";
    private static final String TAG_CLASS = "SettingsFragment";

    public static final String TAG_OF_FRAGMENT = "SettingsFragment";

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
        // set the preference file name, so we can get it more convenience
        this.getPreferenceManager().setSharedPreferencesName(Common.PREFERENCE_MAIN_FILE);
        // set the mode of preference file to world readable, so we can get the
        // settings from the other process
        // TODO use broadcast instead?
        this.getPreferenceManager()
                .setSharedPreferencesMode(PreferenceActivity.MODE_WORLD_READABLE);
        addPreferencesFromResource(R.xml.pref_settings);
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
//        setInsets(this.getActivity(), list);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getTitleRes() == R.string.pref_title_xda) {
            Log.d(TAG, "view the xda thread");
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("http://forum.xda-developers.com/showthread.php?p=50636629"));
            this.getActivity().startActivity(intent);
        }

        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    /*public void setInsets(Activity context, View view) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
            return;
        SystemBarTintManager tintManager = new SystemBarTintManager(context);
        SystemBarTintManager.SystemBarConfig config = tintManager.getConfig();
        view.setPadding(0, config.getPixelInsetTop(true), config.getPixelInsetRight(),
                config.getPixelInsetBottom());
    }*/
}

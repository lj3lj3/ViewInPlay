
package info.daylemk.viewinplay;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

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
}


package info.daylemk.viewinplay;

import android.app.Activity;
import android.os.Bundle;

public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_settings);
        
        SettingsFragment fragment = SettingsFragment.getInstance(null);
        this.getFragmentManager().beginTransaction()
                .replace(R.id.layout_Settings_main,fragment, SettingsFragment.TAG_OF_FRAGMENT).commit();
    }
    
    
}

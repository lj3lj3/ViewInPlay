
package info.daylemk.viewinplay;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;

import com.readystatesoftware.systembartint.SystemBarTintManager;

public class SettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_settings);

        this.getActionBar().setBackgroundDrawable(
                new ColorDrawable(this.getResources().getColor(android.R.color.holo_green_light)));
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            tintManager.setStatusBarTintEnabled(true);
            int actionBarColor = this.getResources().getColor(android.R.color.holo_green_light);
            tintManager.setStatusBarTintColor(actionBarColor);
        }

        SettingsFragment fragment = SettingsFragment.getInstance(null);
        this.getFragmentManager().beginTransaction()
                .replace(R.id.layout_Settings_main, fragment, SettingsFragment.TAG_OF_FRAGMENT)
                .commit();
    }
}

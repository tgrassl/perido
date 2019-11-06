package heliox.com.perido;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Switch;

public class SettingsActivity extends AppCompatActivity {

    public static final String PREFS = "PeridoPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageView backBtn = findViewById(R.id.settingsBtn);
        backBtn.setOnClickListener(y -> {
            Intent settingsIntent = new Intent(getApplicationContext(), HomeActivity.class);
            startActivity(settingsIntent);
        });

        final Switch toggleFolder = findViewById(R.id.toggleFolder);
        final Switch toggleMode = findViewById(R.id.toggleMode);

        final SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = prefs.edit();

        boolean isPathEnabled = prefs.getBoolean(getString(R.string.settings_folder_default), true);
        boolean isTestEnabled = prefs.getBoolean(getString(R.string.settings_testing), true);

        toggleFolder.setChecked(isPathEnabled);
        toggleMode.setChecked(isTestEnabled);

        toggleFolder.setOnClickListener(view -> {
            editor.putBoolean(getString(R.string.settings_folder_default), toggleFolder.isChecked());
            editor.apply();
        });

        toggleMode.setOnClickListener(view -> {
            editor.putBoolean(getString(R.string.settings_testing), toggleMode.isChecked());
            editor.apply();
        });
    }
}

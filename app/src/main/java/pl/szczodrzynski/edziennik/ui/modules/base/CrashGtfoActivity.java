package pl.szczodrzynski.edziennik.ui.modules.base;

import android.content.Context;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;

public class CrashGtfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme((((App)getApplication()).getContext()
                .getSharedPreferences(getString(R.string.preference_file_global), Context.MODE_PRIVATE)
                .getBoolean("dark_theme", false) ? R.style.AppTheme_Dark : R.style.AppTheme));
        setContentView(R.layout.activity_gtfo);
    }
}

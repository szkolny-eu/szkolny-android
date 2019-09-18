package pl.szczodrzynski.edziennik.widgets.timetable;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.MainActivity;
import pl.szczodrzynski.edziennik.WidgetTimetable;
import pl.szczodrzynski.edziennik.dialogs.EventListDialog;
import pl.szczodrzynski.edziennik.models.Date;
import pl.szczodrzynski.edziennik.models.Time;
import pl.szczodrzynski.edziennik.utils.Themes;

import static android.content.Intent.FLAG_ACTIVITY_REORDER_TO_FRONT;

public class LessonDetailsActivity extends AppCompatActivity {
    public LessonDetailsActivity() {
        super();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setBackgroundDrawable(new ColorDrawable(0));

        setTheme(Themes.INSTANCE.getAppThemeNoDisplay());

        App app = (App)getApplication();
        Bundle extras = getIntent().getExtras();
        if (app != null && extras != null) {
            int profileId = extras.getInt("profileId", -1);

            if (extras.getBoolean("separatorItem", false)) {
                Intent i = new Intent(this, MainActivity.class)
                        .putExtra("fragmentId", MainActivity.DRAWER_ITEM_TIMETABLE)
                        .putExtra("profileId", profileId)
                        .addFlags(FLAG_ACTIVITY_REORDER_TO_FRONT);
                app.getContext().startActivity(i);
                finish();
                return;
            }

            Date date = Date.fromYmd(extras.getString("date", "20181109"));
            Time startTime = Time.fromHms(extras.getString("startTime", "20181109"));
            //Time endTime = Time.fromHms(extras.getString("endTime", "20181109"));

            new EventListDialog(this, profileId)
                    .withDismissListener((dialog -> {
                        finish();
                        Intent intent = new Intent(app.getContext(), WidgetTimetable.class);
                        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                        int[] ids = AppWidgetManager.getInstance(app)
                                .getAppWidgetIds(new ComponentName(app, WidgetTimetable.class));
                        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
                        app.sendBroadcast(intent);
                    }))
                    .show(app, date, startTime);
            return;
        }
        Toast.makeText(app, R.string.error_reading_lesson_details, Toast.LENGTH_SHORT).show();
        finish();
    }
}
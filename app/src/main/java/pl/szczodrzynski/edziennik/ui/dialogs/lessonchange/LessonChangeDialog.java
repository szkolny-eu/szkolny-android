package pl.szczodrzynski.edziennik.ui.dialogs.lessonchange;

import android.content.Context;
import android.view.View;

import androidx.databinding.DataBindingUtil;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.ui.modules.timetable.TimetableAdapter;
import pl.szczodrzynski.edziennik.databinding.DialogLessonChangeListBinding;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonFull;
import pl.szczodrzynski.edziennik.utils.models.Date;
import pl.szczodrzynski.edziennik.utils.models.Time;

public class LessonChangeDialog {
    private App app;
    private Context context;
    private int profileId;

    public LessonChangeDialog(Context context) {
        this.context = context;
        this.profileId = App.profileId;
    }
    public LessonChangeDialog(Context context, int profileId) {
        this.context = context;
        this.profileId = profileId;
    }

    private MaterialDialog dialog;
    private DialogLessonChangeListBinding b;

    public void show(App _app, Date date)
    {
        this.app = _app;
        dialog = new MaterialDialog.Builder(context)
                .title(date.getFormattedString())
                .customView(R.layout.dialog_lesson_change_list, false)
                .positiveText(R.string.close)
                .autoDismiss(false)
                .onPositive((dialog, which) -> dialog.dismiss())
                .show();
        if (dialog.getCustomView() == null)
            return;
        b = DataBindingUtil.bind(dialog.getCustomView());
        if (b == null)
            return;

        b.lessonChangeView.setHasFixedSize(true);
        b.lessonChangeView.setLayoutManager(new LinearLayoutManager(context));

        app.db.lessonDao().getAllByDate(profileId, date, Time.getNow()).observe((LifecycleOwner) context, lessons -> {
            if (app == null || app.profile == null || b == null)
                return;

            List<LessonFull> changedLessons = new ArrayList<>();
            for (LessonFull lesson: lessons) {
                if (lesson.changeId != 0) {
                    changedLessons.add(lesson);
                }
            }

            app.db.eventDao().getAllByDate(profileId, date).observe((LifecycleOwner) context, events -> {
                TimetableAdapter adapter = new TimetableAdapter(context, date, changedLessons, events == null ? new ArrayList<>() : events);
                b.lessonChangeView.setAdapter(adapter);
                b.lessonChangeView.setVisibility(View.VISIBLE);
            });
        });
    }
}

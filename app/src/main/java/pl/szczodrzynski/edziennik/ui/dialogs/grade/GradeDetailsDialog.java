package pl.szczodrzynski.edziennik.ui.dialogs.grade;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.AsyncTask;
import android.view.View;

import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.afollestad.materialdialogs.MaterialDialog;

import java.text.DecimalFormat;
import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.db.entity.Grade;
import pl.szczodrzynski.edziennik.data.db.full.GradeFull;
import pl.szczodrzynski.edziennik.databinding.DialogGradeDetailsBinding;
import pl.szczodrzynski.edziennik.ui.modules.grades.GradesListAdapter;
import pl.szczodrzynski.edziennik.utils.Colors;

import static pl.szczodrzynski.edziennik.data.db.entity.Profile.COLOR_MODE_DEFAULT;

public class GradeDetailsDialog {
    private App app;
    private Context context;
    private int profileId;

    public GradeDetailsDialog(Context context) {
        this.context = context;
        this.profileId = App.Companion.getProfileId();
    }
    public GradeDetailsDialog(Context context, int profileId) {
        this.context = context;
        this.profileId = profileId;
    }

    public MaterialDialog dialog;
    private DialogGradeDetailsBinding b;
    private DialogInterface.OnDismissListener dismissListener;
    public boolean callDismissListener = true;

    public GradeDetailsDialog withDismissListener(DialogInterface.OnDismissListener dismissListener) {
        this.dismissListener = dismissListener;
        return this;
    }
    public void performDismiss(DialogInterface dialogInterface) {
        if (callDismissListener && dismissListener != null) {
            dismissListener.onDismiss(dialogInterface);
        }
        callDismissListener = true;
    }

    public void show(App _app, GradeFull grade)
    {
        this.app = _app;
        dialog = new MaterialDialog.Builder(context)
                .customView(R.layout.dialog_grade_details, true)
                .positiveText(R.string.close)
                .autoDismiss(false)
                .onPositive((dialog, which) -> dialog.dismiss())
                .dismissListener(this::performDismiss)
                .show();

        View root = dialog.getCustomView();
        assert root != null;

        b = DialogGradeDetailsBinding.bind(root);

        b.setGrade(grade);

        int gradeColor;
        if (App.Companion.getConfig().getFor(profileId).getGrades().getColorMode() == COLOR_MODE_DEFAULT) {
            gradeColor = grade.color;
        }
        else {
            gradeColor = Colors.gradeToColor(grade);
        }

        DecimalFormat format = new DecimalFormat("#.##");
        if (grade.weight < 0) {
            grade.weight *= -1;
        }
        if (grade.type == Grade.TYPE_DESCRIPTIVE || grade.type == Grade.TYPE_DESCRIPTIVE_TEXT || grade.type == Grade.TYPE_TEXT || grade.type == Grade.TYPE_POINT_SUM) {
            b.setWeightText(null);
            grade.weight = 0;
        }
        else {
            if (grade.type == Grade.TYPE_POINT_AVG) {
                b.setWeightText(app.getString(R.string.grades_max_points_format, format.format(grade.valueMax)));
            }
            else if (grade.weight == 0) {
                b.setWeightText(app.getString(R.string.grades_weight_not_counted));
            }
            else {
                b.setWeightText(app.getString(R.string.grades_weight_format, format.format(grade.weight)));
            }
        }

        b.setCommentVisible(false);

        b.setDevMode(App.Companion.getDevMode());

        b.gradeName.setTextColor(ColorUtils.calculateLuminance(gradeColor) > 0.3 ? 0xff000000 : 0xffffffff);
        b.gradeName.getBackground().setColorFilter(new PorterDuffColorFilter(gradeColor, PorterDuff.Mode.MULTIPLY));

        AsyncTask.execute(() -> {

            List<GradeFull> historyList = app.db.gradeDao().getAllWithParentIdNow(profileId, grade.id);

            if (historyList.size() == 0) {
                b.setHistoryVisible(false);
                return;
            }
            b.setHistoryVisible(true);
            b.gradeHistoryNest.post(() -> {
                b.gradeHistoryNest.setNestedScrollingEnabled(false);
                b.gradeHistoryList.setHasFixedSize(false);
                b.gradeHistoryList.setNestedScrollingEnabled(false);
                b.gradeHistoryList.setLayoutManager(new LinearLayoutManager(context));
                b.gradeHistoryList.setAdapter(new GradesListAdapter(context, historyList));
            });
        });
    }
}

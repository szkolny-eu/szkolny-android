package pl.szczodrzynski.edziennik.ui.modules.grades;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.util.List;

import pl.szczodrzynski.edziennik.App;
import pl.szczodrzynski.edziennik.R;
import pl.szczodrzynski.edziennik.data.db.modules.grades.Grade;
import pl.szczodrzynski.edziennik.data.db.modules.grades.GradeFull;
import pl.szczodrzynski.edziennik.ui.dialogs.grade.GradeDetailsDialog;
import pl.szczodrzynski.edziennik.utils.models.Date;
import pl.szczodrzynski.edziennik.utils.Colors;

import static pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile.COLOR_MODE_DEFAULT;

public class GradesListAdapter extends RecyclerView.Adapter<GradesListAdapter.ViewHolder> {
    private Context mContext;
    private List<GradeFull> gradeList;

    //getting the context and product list with constructor
    public GradesListAdapter(Context mCtx, List<GradeFull> gradeList) {
        this.mContext = mCtx;
        this.gradeList = gradeList;
    }

    @NonNull
    @Override
    public GradesListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //inflating and returning our view holder
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.row_grades_list_item, parent, false);
        return new GradesListAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GradesListAdapter.ViewHolder holder, int position) {
        App app = (App) mContext.getApplicationContext();

        GradeFull grade = gradeList.get(position);

        holder.root.setOnClickListener((v -> {
            new GradeDetailsDialog(v.getContext(), App.profileId).show(app, grade);
        }));

        int gradeColor;
        if (app.profile.getGradeColorMode() == COLOR_MODE_DEFAULT) {
            gradeColor = grade.color;
        }
        else {
            gradeColor = Colors.gradeToColor(grade);
        }

        holder.gradesListName.setText(grade.name);
        holder.gradesListName.setSelected(true);
        holder.gradesListName.setTypeface(null, Typeface.BOLD);
        holder.gradesListName.setTextColor(ColorUtils.calculateLuminance(gradeColor) > 0.25 ? 0xff000000 : 0xffffffff);
        holder.gradesListName.getBackground().setColorFilter(new PorterDuffColorFilter(gradeColor, PorterDuff.Mode.MULTIPLY));

        if (grade.description.trim().isEmpty()) {
            holder.gradesListDescription.setText(grade.category);
            holder.gradesListCategory.setText(grade.isImprovement ? app.getString(R.string.grades_improvement_category_format, "") : "");
        }
        else {
            holder.gradesListDescription.setText(grade.description);
            holder.gradesListCategory.setText(grade.isImprovement ? app.getString(R.string.grades_improvement_category_format, grade.category) : grade.category);
        }

        DecimalFormat format = new DecimalFormat("#.##");
        DecimalFormat formatWithZeroes = new DecimalFormat("#.00");

        if (grade.weight < 0) {
            grade.weight *= -1;
        }
        if (grade.type == Grade.TYPE_DESCRIPTIVE || grade.type == Grade.TYPE_TEXT || grade.type == Grade.TYPE_BEHAVIOUR) {
            holder.gradesListWeight.setVisibility(View.GONE);
            grade.weight = 0;
        }
        else {
            holder.gradesListWeight.setVisibility(View.VISIBLE);
            if (grade.type == Grade.TYPE_POINT) {
                holder.gradesListWeight.setText(app.getString(R.string.grades_max_points_format, format.format(grade.valueMax)));
            }
            else if (grade.weight == 0) {
                holder.gradesListWeight.setText(app.getString(R.string.grades_weight_not_counted));
            }
            else {
                holder.gradesListWeight.setText(app.getString(R.string.grades_weight_format, format.format(grade.weight) + (grade.classAverage != -1 ? ", " + formatWithZeroes.format(grade.classAverage) : "")));
            }
        }


        holder.gradesListTeacher.setText(grade.teacherFullName);
        holder.gradesListAddedDate.setText(Date.fromMillis(grade.addedDate).getFormattedStringShort());

        if (!grade.seen) {
            holder.gradesListDescription.setBackground(mContext.getResources().getDrawable(R.drawable.bg_rounded_4dp));
            holder.gradesListDescription.getBackground().setColorFilter(new PorterDuffColorFilter(0x692196f3, PorterDuff.Mode.MULTIPLY));
        }
        else {
            holder.gradesListDescription.setBackground(null);
        }
    }

    @Override
    public int getItemCount() {
        return gradeList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        View root;
        TextView gradesListName;
        TextView gradesListDescription;
        TextView gradesListCategory;
        TextView gradesListWeight;
        TextView gradesListTeacher;
        TextView gradesListAddedDate;

        ViewHolder(View itemView) {
            super(itemView);
            root = itemView.getRootView();
            gradesListName = itemView.findViewById(R.id.gradesListName);
            gradesListDescription = itemView.findViewById(R.id.gradesListCategoryColumn);
            gradesListCategory = itemView.findViewById(R.id.gradesListCategoryDescription);
            gradesListWeight = itemView.findViewById(R.id.gradesListWeight);
            gradesListTeacher = itemView.findViewById(R.id.gradesListTeacher);
            gradesListAddedDate = itemView.findViewById(R.id.gradesListAddedDate);
        }
    }
}


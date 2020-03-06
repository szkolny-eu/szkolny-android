/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-29.
 */

package pl.szczodrzynski.edziennik.ui.modules.grades.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.full.GradeFull
import pl.szczodrzynski.edziennik.databinding.GradesItemGradeBinding
import pl.szczodrzynski.edziennik.utils.models.Date

class GradeViewHolder(
    inflater: LayoutInflater,
    parent: ViewGroup,
    val b: GradesItemGradeBinding = GradesItemGradeBinding.inflate(inflater, parent, false)
) : RecyclerView.ViewHolder(b.root), BindableViewHolder<GradeFull> {
    companion object {
        private const val TAG = "GradeViewHolder"
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun onBind(activity: AppCompatActivity, app: App, grade: GradeFull, position: Int) {
        val manager = app.gradesManager

        b.gradeName.setGrade(grade, manager, bigView = true)

        if (grade.description.isNullOrBlank()) {
            b.gradeDescription.text = grade.category
            b.gradeCategory.text =
                if (grade.isImprovement)
                    app.getString(R.string.grades_improvement_category_format, "")
                else
                    ""
        } else {
            b.gradeDescription.text = grade.description
            b.gradeCategory.text =
                if (grade.isImprovement)
                    app.getString(R.string.grades_improvement_category_format, grade.category)
                else
                    grade.category
        }

        b.gradeWeight.text = manager.getWeightString(activity, grade, showClassAverage = true)

        b.gradeTeacherName.text = grade.teacherFullName
        b.gradeAddedDate.text = Date.fromMillis(grade.addedDate).let {
            it.getRelativeString(app, 5) ?: it.formattedStringShort
        }

        /*if (!grade.seen) {
            b.gradeDescription.setBackground(mContext.getResources().getDrawable(R.drawable.bg_rounded_4dp))
            b.gradeDescription.getBackground()
                .setColorFilter(PorterDuffColorFilter(0x692196f3, PorterDuff.Mode.MULTIPLY))
        } else {
            b.gradeDescription.setBackground(null)
        }*/
    }
}

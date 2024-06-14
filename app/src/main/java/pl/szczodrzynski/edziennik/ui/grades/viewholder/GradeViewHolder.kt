/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-29.
 */

package pl.szczodrzynski.edziennik.ui.grades.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.full.GradeFull
import pl.szczodrzynski.edziennik.databinding.GradesItemGradeBinding
import pl.szczodrzynski.edziennik.ui.grades.GradesAdapter
import pl.szczodrzynski.edziennik.ui.grades.models.GradesSubject
import pl.szczodrzynski.edziennik.utils.managers.NoteManager
import pl.szczodrzynski.edziennik.utils.models.Date

class GradeViewHolder(
    inflater: LayoutInflater,
    parent: ViewGroup,
    val b: GradesItemGradeBinding = GradesItemGradeBinding.inflate(inflater, parent, false)
) : RecyclerView.ViewHolder(b.root), BindableViewHolder<GradeFull, GradesAdapter> {
    companion object {
        private const val TAG = "GradeViewHolder"
    }

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override fun onBind(activity: AppCompatActivity, app: App, grade: GradeFull, position: Int, adapter: GradesAdapter) {
        val manager = app.gradesManager
        val gradeCategoryText: String = when {
            grade.category != null && grade.code != null -> "${grade.code} - ${grade.category}"
            grade.code != null -> grade.code!!
            else -> grade.category!!
        }

        b.gradeName.setGrade(grade, manager, bigView = true)

        if (grade.description.isNullOrBlank()) {
            b.gradeDescription.text =
                grade.getNoteSubstituteText(adapter.showNotes) ?: grade.category
            b.gradeCategory.text =
                if (grade.isImprovement)
                    app.getString(R.string.grades_improvement_category_format, "")
                else
                    if (grade.code != null) gradeCategoryText else null
        } else {
            b.gradeDescription.text =
                grade.getNoteSubstituteText(adapter.showNotes) ?: grade.description
            b.gradeCategory.text =
                if (grade.isImprovement)
                    app.getString(R.string.grades_improvement_category_format, gradeCategoryText)
                else
                    gradeCategoryText
        }

        if (adapter.showNotes)
            NoteManager.prependIcon(grade, b.gradeDescription)

        val weightText = manager.getWeightString(activity, grade, showClassAverage = true)
        b.gradeWeight.text = weightText
        b.gradeWeight.isVisible = weightText != null

        b.gradeTeacherName.text = grade.teacherName
        b.gradeAddedDate.text = Date.fromMillis(grade.addedDate).let {
            it.getRelativeString(app, 5) ?: it.formattedStringShort
        }

        b.unread.isVisible = grade.showAsUnseen
        if (!grade.seen) {
            manager.markAsSeen(grade)
            val subject = adapter.items.firstOrNull {
                it is GradesSubject && it.subjectId == grade.subjectId
            } as? GradesSubject ?: return

            val semester = subject.semesters.firstOrNull { it.number == grade.semester } ?: return

            semester.hasUnseen = semester.grades.any { !it.seen }
            // check if the unseen status has changed
            if (!semester.hasUnseen) {
                adapter.notifyItemChanged(semester)
            }
            if (!subject.hasUnseen) {
                adapter.notifyItemChanged(subject)
            }
            if (manager.hideImproved && grade.isImproved) {
                adapter.removeItem(grade)
            }
        }
    }
}

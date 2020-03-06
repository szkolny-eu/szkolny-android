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
import pl.szczodrzynski.edziennik.databinding.GradesItemSemesterBinding
import pl.szczodrzynski.edziennik.setText
import pl.szczodrzynski.edziennik.ui.modules.grades.GradesAdapter
import pl.szczodrzynski.edziennik.ui.modules.grades.models.GradesSemester

class SemesterViewHolder(
    inflater: LayoutInflater,
    parent: ViewGroup,
    val b: GradesItemSemesterBinding = GradesItemSemesterBinding.inflate(inflater, parent, false)
) : RecyclerView.ViewHolder(b.root), BindableViewHolder<GradesSemester> {
    companion object {
        private const val TAG = "SemesterViewHolder"
    }

    override fun onBind(activity: AppCompatActivity, app: App, item: GradesSemester, position: Int) {
        val manager = app.gradesManager
        b.semesterName.setText(R.string.grades_semester_format, item.number)
        b.dropdownIcon.rotation = when (item.state) {
            GradesAdapter.STATE_CLOSED -> 0f
            else -> 180f
        }

        b.average.text = manager.getAverageString(app, item.averages)
        b.proposedGrade.setGrade(item.proposedGrade, manager)
        b.finalGrade.setGrade(item.finalGrade, manager)
    }
}

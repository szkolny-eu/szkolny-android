/*
 * Copyright (c) Kuba Szczodrzyński 2020-5-4.
 */

package pl.szczodrzynski.edziennik.ui.attendance.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.AttendanceItemSubjectBinding
import pl.szczodrzynski.edziennik.ext.setText
import pl.szczodrzynski.edziennik.ui.attendance.AttendanceAdapter
import pl.szczodrzynski.edziennik.ui.attendance.AttendanceAdapter.Companion.STATE_CLOSED
import pl.szczodrzynski.edziennik.ui.attendance.models.AttendanceSubject
import pl.szczodrzynski.edziennik.ui.grades.viewholder.BindableViewHolder

class SubjectViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup,
        val b: AttendanceItemSubjectBinding = AttendanceItemSubjectBinding.inflate(inflater, parent, false)
) : RecyclerView.ViewHolder(b.root), BindableViewHolder<AttendanceSubject, AttendanceAdapter> {
    companion object {
        private const val TAG = "SubjectViewHolder"
    }

    override fun onBind(activity: AppCompatActivity, app: App, item: AttendanceSubject, position: Int, adapter: AttendanceAdapter) {
        val manager = app.attendanceManager

        b.title.text = item.subjectName

        b.dropdownIcon.rotation = when (item.state) {
            STATE_CLOSED -> 0f
            else -> 180f
        }

        b.unread.isVisible = item.hasUnseen

        b.attendanceBar.setAttendanceData(item.typeCountMap.map { manager.getAttendanceColor(it.key) to it.value })

        b.percentage.isVisible = true

        if (item.percentage == 0f) {
            b.percentage.isVisible = false
            b.percentage.text = null
        }
        else {
            b.percentage.setText(R.string.attendance_percentage_format, item.percentage)

            if(manager.showDifference && item.presenceDifference < 10){
                val differenceText = if(item.presenceDifference > 0)
                    "+" + item.presenceDifference
                else
                    item.presenceDifference

                b.percentage.setText(b.percentage.text.toString() + " | " + differenceText);
            }
        }
    }
}

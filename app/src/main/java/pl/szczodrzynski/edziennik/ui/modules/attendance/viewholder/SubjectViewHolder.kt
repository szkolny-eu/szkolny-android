/*
 * Copyright (c) Kuba Szczodrzyński 2020-5-4.
 */

package pl.szczodrzynski.edziennik.ui.modules.attendance.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.AttendanceItemContainerSubjectBinding
import pl.szczodrzynski.edziennik.setText
import pl.szczodrzynski.edziennik.ui.modules.attendance.AttendanceAdapter
import pl.szczodrzynski.edziennik.ui.modules.attendance.AttendanceAdapter.Companion.STATE_CLOSED
import pl.szczodrzynski.edziennik.ui.modules.attendance.models.AttendanceSubject
import pl.szczodrzynski.edziennik.ui.modules.grades.viewholder.BindableViewHolder
import pl.szczodrzynski.edziennik.utils.Themes

class SubjectViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup,
        val b: AttendanceItemContainerSubjectBinding = AttendanceItemContainerSubjectBinding.inflate(inflater, parent, false)
) : RecyclerView.ViewHolder(b.root), BindableViewHolder<AttendanceSubject, AttendanceAdapter> {
    companion object {
        private const val TAG = "SubjectViewHolder"
    }

    override fun onBind(activity: AppCompatActivity, app: App, item: AttendanceSubject, position: Int, adapter: AttendanceAdapter) {
        val manager = app.attendanceManager
        val contextWrapper = ContextThemeWrapper(activity, Themes.appTheme)

        b.title.text = item.subjectName

        b.dropdownIcon.rotation = when (item.state) {
            STATE_CLOSED -> 0f
            else -> 180f
        }

        b.unread.isVisible = item.hasUnseen

        b.attendanceBar.setAttendanceData(item.typeCountMap.mapKeys { manager.getAttendanceColor(it.key) })

        b.percentage.isVisible = true

        if (item.percentage == 0f) {
            b.percentage.isVisible = false
            b.percentage.text = null
        }
        else {
            b.percentage.setText(R.string.attendance_percentage_format, item.percentage)
        }
    }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-30.
 */

package pl.szczodrzynski.edziennik.ui.modules.attendance.viewholder

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.concat
import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.databinding.AttendanceItemDayRangeBinding
import pl.szczodrzynski.edziennik.ui.modules.attendance.AttendanceAdapter
import pl.szczodrzynski.edziennik.ui.modules.attendance.AttendanceAdapter.Companion.STATE_CLOSED
import pl.szczodrzynski.edziennik.ui.modules.attendance.AttendanceView
import pl.szczodrzynski.edziennik.ui.modules.attendance.models.AttendanceDayRange
import pl.szczodrzynski.edziennik.ui.modules.grades.viewholder.BindableViewHolder
import pl.szczodrzynski.edziennik.utils.Themes

class DayRangeViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup,
        val b: AttendanceItemDayRangeBinding = AttendanceItemDayRangeBinding.inflate(inflater, parent, false)
) : RecyclerView.ViewHolder(b.root), BindableViewHolder<AttendanceDayRange, AttendanceAdapter> {
    companion object {
        private const val TAG = "DayRangeViewHolder"
    }

    override fun onBind(activity: AppCompatActivity, app: App, item: AttendanceDayRange, position: Int, adapter: AttendanceAdapter) {
        val manager = app.attendanceManager
        val contextWrapper = ContextThemeWrapper(activity, Themes.appTheme)

        b.title.text = listOf(
                item.rangeStart.formattedString,
                item.rangeEnd?.formattedString
        ).concat(" - ")

        b.dropdownIcon.rotation = when (item.state) {
            STATE_CLOSED -> 0f
            else -> 180f
        }

        b.unread.isVisible = item.hasUnseen

        b.previewContainer.visibility = if (item.state == STATE_CLOSED) View.VISIBLE else View.INVISIBLE
        b.summaryContainer.visibility = if (item.state == STATE_CLOSED) View.INVISIBLE else View.VISIBLE

        b.previewContainer.removeAllViews()

        for (attendance in item.items) {
            if (attendance.baseType == Attendance.TYPE_PRESENT_CUSTOM || attendance.baseType == Attendance.TYPE_UNKNOWN)
                continue
            b.previewContainer.addView(AttendanceView(
                    contextWrapper,
                    attendance,
                    manager
            ))
        }
        if (item.items.isEmpty() || item.items.none { it.baseType != Attendance.TYPE_PRESENT_CUSTOM && it.baseType != Attendance.TYPE_UNKNOWN }) {
            b.previewContainer.addView(TextView(contextWrapper).also {
                it.setText(R.string.attendance_empty_text)
            })
        }
    }
}

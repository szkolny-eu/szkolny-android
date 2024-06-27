/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-30.
 */

package pl.szczodrzynski.edziennik.ui.attendance.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.databinding.AttendanceItemMonthBinding
import pl.szczodrzynski.edziennik.ext.concat
import pl.szczodrzynski.edziennik.ext.dp
import pl.szczodrzynski.edziennik.ext.fixName
import pl.szczodrzynski.edziennik.ext.setText
import pl.szczodrzynski.edziennik.ui.attendance.AttendanceAdapter
import pl.szczodrzynski.edziennik.ui.attendance.AttendanceAdapter.Companion.STATE_CLOSED
import pl.szczodrzynski.edziennik.ui.attendance.AttendanceView
import pl.szczodrzynski.edziennik.ui.attendance.models.AttendanceMonth
import pl.szczodrzynski.edziennik.ui.grades.viewholder.BindableViewHolder
import pl.szczodrzynski.edziennik.utils.Themes
import pl.szczodrzynski.edziennik.utils.models.Date

class MonthViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup,
        val b: AttendanceItemMonthBinding = AttendanceItemMonthBinding.inflate(inflater, parent, false)
) : RecyclerView.ViewHolder(b.root), BindableViewHolder<AttendanceMonth, AttendanceAdapter> {
    companion object {
        private const val TAG = "MonthViewHolder"
    }

    override fun onBind(activity: AppCompatActivity, app: App, item: AttendanceMonth, position: Int, adapter: AttendanceAdapter) {
        val manager = app.attendanceManager
        val contextWrapper = app.uiManager.getContextWrapper(activity)

        b.title.text = listOf(
                app.resources.getStringArray(R.array.material_calendar_months_array).getOrNull(item.month - 1)?.fixName(),
                item.year.toString()
        ).concat(" ")

        b.dropdownIcon.rotation = when (item.state) {
            STATE_CLOSED -> 0f
            else -> 180f
        }

        b.unread.isVisible = item.hasUnseen

        b.attendanceBar.setAttendanceData(item.typeCountMap.map { manager.getAttendanceColor(it.key) to it.value })

        b.previewContainer.isInvisible = item.state != STATE_CLOSED
        b.summaryContainer.isInvisible = item.state == STATE_CLOSED
        b.percentage.isVisible = item.state == STATE_CLOSED

        b.previewContainer.removeAllViews()

        item.typeCountMap.forEach { (type, count) ->
            val layout = LinearLayout(contextWrapper)
            val attendance = Attendance(
                    profileId = 0,
                    id = 0,
                    baseType = type.baseType,
                    typeName = "",
                    typeShort = type.typeShort,
                    typeSymbol = type.typeSymbol,
                    typeColor = type.typeColor,
                    date = Date(0, 0, 0),
                    startTime = null,
                    semester = 0,
                    teacherId = 0,
                    subjectId = 0,
                    addedDate = 0
            )
            layout.addView(AttendanceView(contextWrapper, attendance, manager))
            layout.addView(TextView(contextWrapper).also {
                //it.setText(R.string.attendance_percentage_format, count/sum*100f)
                it.text = count.toString()
                it.setPadding(0, 0, 5.dp, 0)
            })
            layout.setPadding(0, 8.dp, 0, 0)
            b.previewContainer.addView(layout)
        }

        if (item.percentage == 0f) {
            b.percentage.isVisible = false
            b.percentage.text = null
            b.summaryContainer.isVisible = false
            b.summaryContainer.text = null
        }
        else {
            b.percentage.setText(R.string.attendance_percentage_format, item.percentage)
            b.summaryContainer.setText(R.string.attendance_period_summary_format, item.percentage)
        }
    }
}

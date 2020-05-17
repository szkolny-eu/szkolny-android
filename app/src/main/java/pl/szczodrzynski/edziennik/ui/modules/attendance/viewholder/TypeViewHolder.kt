/*
 * Copyright (c) Kuba Szczodrzyński 2020-5-8.
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
import pl.szczodrzynski.edziennik.concat
import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.databinding.AttendanceItemTypeBinding
import pl.szczodrzynski.edziennik.ui.modules.attendance.AttendanceAdapter
import pl.szczodrzynski.edziennik.ui.modules.attendance.models.AttendanceTypeGroup
import pl.szczodrzynski.edziennik.ui.modules.grades.viewholder.BindableViewHolder
import pl.szczodrzynski.edziennik.utils.Themes
import pl.szczodrzynski.edziennik.utils.models.Date

class TypeViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup,
        val b: AttendanceItemTypeBinding = AttendanceItemTypeBinding.inflate(inflater, parent, false)
) : RecyclerView.ViewHolder(b.root), BindableViewHolder<AttendanceTypeGroup, AttendanceAdapter> {
    companion object {
        private const val TAG = "TypeViewHolder"
    }

    override fun onBind(activity: AppCompatActivity, app: App, item: AttendanceTypeGroup, position: Int, adapter: AttendanceAdapter) {
        val manager = app.attendanceManager
        val contextWrapper = ContextThemeWrapper(activity, Themes.appTheme)

        val type = item.type
        b.title.text = type.typeName

        b.dropdownIcon.rotation = when (item.state) {
            AttendanceAdapter.STATE_CLOSED -> 0f
            else -> 180f
        }

        b.unread.isVisible = item.hasUnseen

        b.details.text = listOf(
                app.getString(R.string.attendance_percentage_format, item.percentage),
                app.getString(R.string.attendance_type_yearly_format, item.items.size),
                app.getString(R.string.attendance_type_semester_format, item.semesterCount)
        ).concat(" • ")

        b.type.setAttendance(Attendance(
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
        ), manager, bigView = false)
    }
}

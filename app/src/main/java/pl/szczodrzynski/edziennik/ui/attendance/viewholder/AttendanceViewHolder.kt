/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-30.
 */

package pl.szczodrzynski.edziennik.ui.attendance.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.full.AttendanceFull
import pl.szczodrzynski.edziennik.databinding.AttendanceItemAttendanceBinding
import pl.szczodrzynski.edziennik.ext.concat
import pl.szczodrzynski.edziennik.ui.attendance.AttendanceAdapter
import pl.szczodrzynski.edziennik.ui.attendance.models.AttendanceDayRange
import pl.szczodrzynski.edziennik.ui.attendance.models.AttendanceMonth
import pl.szczodrzynski.edziennik.ui.grades.models.ExpandableItemModel
import pl.szczodrzynski.edziennik.ui.grades.viewholder.BindableViewHolder
import pl.szczodrzynski.edziennik.core.manager.NoteManager
import pl.szczodrzynski.edziennik.utils.models.Week

class AttendanceViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup,
        val b: AttendanceItemAttendanceBinding = AttendanceItemAttendanceBinding.inflate(inflater, parent, false)
) : RecyclerView.ViewHolder(b.root), BindableViewHolder<AttendanceFull, AttendanceAdapter> {
    companion object {
        private const val TAG = "AttendanceViewHolder"
    }

    override fun onBind(activity: AppCompatActivity, app: App, item: AttendanceFull, position: Int, adapter: AttendanceAdapter) {
        val manager = app.attendanceManager

        val bullet = " • "

        b.attendanceView.setAttendance(item, manager, bigView = true)

        b.type.text = item.typeName
        b.subjectName.text = item.getNoteSubstituteText(adapter.showNotes) ?: item.subjectLongName
                ?: item.lessonTopic
        if (adapter.showNotes)
            NoteManager.prependIcon(item, b.subjectName)
        b.dateTime.text = listOf(
                Week.getFullDayName(item.date.weekDay),
                item.date.formattedStringShort,
                item.startTime?.stringHM,
                item.lessonNumber?.let { app.getString(R.string.attendance_lesson_number_format, it) }
        ).concat(bullet)

        if (item.showAsUnseen == null)
            item.showAsUnseen = !item.seen

        b.unread.isVisible = item.showAsUnseen == true
        if (!item.seen) {
            manager.markAsSeen(item)

            val container = adapter.items.firstOrNull {
                it is ExpandableItemModel<*> && it.items.contains(item)
            } as? ExpandableItemModel<*> ?: return

            var hasUnseen = true
            if (container is AttendanceDayRange) {
                hasUnseen = container.items.any { !it.seen }
                container.hasUnseen = hasUnseen
            }
            if (container is AttendanceMonth) {
                hasUnseen = container.items.any { !it.seen }
                container.hasUnseen = hasUnseen
            }

            // check if the unseen status has changed
            if (!hasUnseen) {
                adapter.notifyItemChanged(container)
            }
        }
    }
}

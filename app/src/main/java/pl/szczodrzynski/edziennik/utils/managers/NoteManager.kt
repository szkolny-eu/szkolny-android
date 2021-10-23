/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.utils.managers

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.entity.Noteable
import pl.szczodrzynski.edziennik.data.db.full.*
import pl.szczodrzynski.edziennik.ui.agenda.DayDialog
import pl.szczodrzynski.edziennik.ui.agenda.lessonchanges.LessonChangesAdapter
import pl.szczodrzynski.edziennik.ui.announcements.AnnouncementsAdapter
import pl.szczodrzynski.edziennik.ui.attendance.AttendanceAdapter
import pl.szczodrzynski.edziennik.ui.attendance.AttendanceDetailsDialog
import pl.szczodrzynski.edziennik.ui.attendance.AttendanceFragment
import pl.szczodrzynski.edziennik.ui.behaviour.NoticesAdapter
import pl.szczodrzynski.edziennik.ui.event.EventDetailsDialog
import pl.szczodrzynski.edziennik.ui.event.EventListAdapter
import pl.szczodrzynski.edziennik.ui.grades.GradeDetailsDialog
import pl.szczodrzynski.edziennik.ui.grades.GradesAdapter
import pl.szczodrzynski.edziennik.ui.messages.list.MessagesAdapter
import pl.szczodrzynski.edziennik.ui.timetable.LessonDetailsDialog
import pl.szczodrzynski.edziennik.utils.models.Date

class NoteManager(private val app: App) {
    companion object {
        private const val TAG = "NoteManager"
    }

    fun getAdapterForItem(activity: AppCompatActivity, item: Noteable): RecyclerView.Adapter<*>? {
        return when (item) {
            is AnnouncementFull -> AnnouncementsAdapter(activity, mutableListOf(item), null)

            is AttendanceFull -> AttendanceAdapter(activity, onAttendanceClick = {
                showItemDetailsDialog(activity, it)
            }, type = AttendanceFragment.VIEW_LIST).also {
                it.items = mutableListOf(item)
            }

            is NoticeFull -> {
                NoticesAdapter(activity, listOf(item))
            }

            is Date -> {
                TODO("Date adapter is not yet implemented.")
            }

            is EventFull -> EventListAdapter(
                activity = activity,
                simpleMode = true,
                showDate = true,
                showTime = false,
                markAsSeen = false,
                onEventClick = {
                    showItemDetailsDialog(activity, it)
                },
            ).also {
                it.setAllItems(listOf(item))
            }

            is GradeFull -> GradesAdapter(activity, onGradeClick = {
                showItemDetailsDialog(activity, it)
            }).also {
                it.items = mutableListOf(item)
            }

            is LessonFull -> LessonChangesAdapter(activity, onLessonClick = {
                showItemDetailsDialog(activity, it)
            }).also {
                it.items = listOf(item)
            }

            is MessageFull -> MessagesAdapter(
                activity = activity,
                teachers = listOf(),
                onMessageClick = null,
            ).also {
                it.setAllItems(listOf(item))
            }
            else -> null
        }
    }

    fun showItemDetailsDialog(
        activity: AppCompatActivity,
        item: Noteable,
        onShowListener: ((tag: String) -> Unit)? = null,
        onDismissListener: ((tag: String) -> Unit)? = null,
    ) {
        when (item) {
            is AnnouncementFull -> return
            is AttendanceFull -> AttendanceDetailsDialog(
                activity = activity,
                attendance = item,
                showNotesButton = false,
                onShowListener = onShowListener,
                onDismissListener = onDismissListener,
            ).show()
            is NoticeFull -> return
            is Date -> DayDialog(
                activity = activity,
                profileId = App.profileId,
                date = item,
                showNotesButton = false,
                onShowListener = onShowListener,
                onDismissListener = onDismissListener,
            ).show()
            is EventFull -> EventDetailsDialog(
                activity = activity,
                event = item,
                showNotesButton = false,
                onShowListener = onShowListener,
                onDismissListener = onDismissListener,
            ).show()
            is GradeFull -> GradeDetailsDialog(
                activity = activity,
                grade = item,
                showNotesButton = false,
                onShowListener = onShowListener,
                onDismissListener = onDismissListener,
            ).show()
            is LessonFull -> LessonDetailsDialog(
                activity = activity,
                lesson = item,
                showNotesButton = false,
                onShowListener = onShowListener,
                onDismissListener = onDismissListener,
            ).show()
            is MessageFull -> return
        }
    }
}

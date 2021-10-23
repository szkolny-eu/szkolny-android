/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.utils.managers

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.entity.Noteable
import pl.szczodrzynski.edziennik.data.db.full.*
import pl.szczodrzynski.edziennik.ui.agenda.lessonchanges.LessonChangesAdapter
import pl.szczodrzynski.edziennik.ui.announcements.AnnouncementsAdapter
import pl.szczodrzynski.edziennik.ui.attendance.AttendanceAdapter
import pl.szczodrzynski.edziennik.ui.attendance.AttendanceFragment
import pl.szczodrzynski.edziennik.ui.behaviour.NoticesAdapter
import pl.szczodrzynski.edziennik.ui.event.EventListAdapter
import pl.szczodrzynski.edziennik.ui.grades.GradesAdapter
import pl.szczodrzynski.edziennik.ui.messages.list.MessagesAdapter
import pl.szczodrzynski.edziennik.utils.models.Date

class NoteManager(private val app: App) {
    companion object {
        private const val TAG = "NoteManager"
    }

    fun getAdapterForItem(activity: AppCompatActivity, item: Noteable): RecyclerView.Adapter<*>? {
        return when (item) {
            is AnnouncementFull -> {
                AnnouncementsAdapter(activity, mutableListOf(item)) { _, it ->
                    showItemDetailsDialog(activity, it)
                }
            }
            is AttendanceFull -> {
                AttendanceAdapter(activity, onAttendanceClick = {
                    showItemDetailsDialog(activity, it)
                }, type = AttendanceFragment.VIEW_LIST).also {
                    it.items = mutableListOf(item)
                }
            }
            is NoticeFull -> {
                NoticesAdapter(activity, listOf(item))
            }
            is Date -> {
                TODO("Date adapter is not yet implemented.")
            }
            is EventFull -> {
                EventListAdapter(
                    activity = activity,
                    simpleMode = true,
                    showWeekDay = false,
                    showDate = true,
                    showType = true,
                    showTime = false,
                    showSubject = true,
                    markAsSeen = false,
                    isReversed = false,
                    onItemClick = {
                        showItemDetailsDialog(activity, it)
                    },
                ).also {
                    it.setAllItems(listOf(item))
                }
            }
            is GradeFull -> {
                GradesAdapter(activity, onGradeClick = {
                    showItemDetailsDialog(activity, it)
                }).also {
                    it.items = mutableListOf(item)
                }
            }
            is LessonFull -> {
                LessonChangesAdapter(activity, onItemClick = {
                    showItemDetailsDialog(activity, it)
                }).also {
                    it.items = listOf(item)
                }
            }
            is MessageFull -> {
                MessagesAdapter(activity, teachers = listOf(), onItemClick = {
                    showItemDetailsDialog(activity, it)
                }).also {
                    it.setAllItems(listOf(item))
                }
            }
            else -> null
        }
    }

    fun showItemDetailsDialog(activity: AppCompatActivity, item: Noteable) {
        Toast.makeText(activity, item.toString(), Toast.LENGTH_SHORT).show()
    }
}

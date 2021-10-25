/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.utils.managers

import android.annotation.SuppressLint
import android.text.SpannableStringBuilder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.iconics.view.IconicsTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Note
import pl.szczodrzynski.edziennik.data.db.entity.Noteable
import pl.szczodrzynski.edziennik.data.db.full.*
import pl.szczodrzynski.edziennik.databinding.NoteDialogHeaderBinding
import pl.szczodrzynski.edziennik.ext.resolveDrawable
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

        @SuppressLint("SetTextI18n")
        fun prependIcon(owner: Noteable, textView: IconicsTextView) {
            if (owner.hasNotes())
                textView.text = SpannableStringBuilder(
                    if (owner.hasReplacingNotes())
                        "{cmd-swap-horizontal} "
                    else
                        "{cmd-playlist-edit} "
                ).append(textView.text)
        }

        fun getLegendText(owner: Noteable): Int? = when {
            owner.hasReplacingNotes() -> R.string.legend_notes_added_replaced
            owner.hasNotes() -> R.string.legend_notes_added
            else -> null
        }

        fun setLegendText(owner: Noteable, textView: IconicsTextView) {
            textView.isVisible = owner.hasNotes()
            textView.setText(getLegendText(owner) ?: return)
        }
    }

    suspend fun saveNote(note: Note, wasShared: Boolean) {
        if (!note.isShared && wasShared) {
            unshareNote(note)
        } else if (note.isShared) {
            shareNote(note)
        }

        withContext(Dispatchers.IO) {
            app.db.noteDao().add(note)
        }
    }

    suspend fun deleteNote(note: Note) {
        if (note.isShared) {
            unshareNote(note)
        }

        withContext(Dispatchers.IO) {
            app.db.noteDao().delete(note)
        }
    }

    private suspend fun shareNote(note: Note) {

    }

    private suspend fun unshareNote(note: Note) {

    }

    fun getAdapterForItem(activity: AppCompatActivity, item: Noteable): RecyclerView.Adapter<*>? {
        return when (item) {
            is AnnouncementFull -> AnnouncementsAdapter(activity, mutableListOf(item), null)

            is AttendanceFull -> AttendanceAdapter(
                activity,
                showNotes = false,
                onAttendanceClick = {
                    showItemDetailsDialog(activity, it)
                },
                type = AttendanceFragment.VIEW_LIST
            ).also {
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
                showColor = false,
                showTime = false,
                markAsSeen = false,
                showNotes = false,
                onEventClick = {
                    showItemDetailsDialog(activity, it)
                },
            ).also {
                it.setAllItems(listOf(item))
            }

            is GradeFull -> GradesAdapter(activity, showNotes = false, onGradeClick = {
                showItemDetailsDialog(activity, it)
            }).also {
                it.items = mutableListOf(item)
            }

            is LessonFull -> LessonChangesAdapter(activity, showNotes = false, onLessonClick = {
                showItemDetailsDialog(activity, it)
            }).also {
                it.items = listOf(item)
            }

            is MessageFull -> MessagesAdapter(
                activity = activity,
                teachers = listOf(),
                showNotes = false,
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
                showNotes = false,
                onShowListener = onShowListener,
                onDismissListener = onDismissListener,
            ).show()
            is NoticeFull -> return
            is Date -> DayDialog(
                activity = activity,
                profileId = App.profileId,
                date = item,
                showNotes = false,
                onShowListener = onShowListener,
                onDismissListener = onDismissListener,
            ).show()
            is EventFull -> EventDetailsDialog(
                activity = activity,
                event = item,
                showNotes = false,
                onShowListener = onShowListener,
                onDismissListener = onDismissListener,
            ).show()
            is GradeFull -> GradeDetailsDialog(
                activity = activity,
                grade = item,
                showNotes = false,
                onShowListener = onShowListener,
                onDismissListener = onDismissListener,
            ).show()
            is LessonFull -> LessonDetailsDialog(
                activity = activity,
                lesson = item,
                showNotes = false,
                onShowListener = onShowListener,
                onDismissListener = onDismissListener,
            ).show()
            is MessageFull -> return
        }
    }

    fun configureHeader(
        activity: AppCompatActivity,
        noteOwner: Noteable,
        b: NoteDialogHeaderBinding,
    ) {
        b.ownerItemList.apply {
            adapter = getAdapterForItem(activity, noteOwner)
            isNestedScrollingEnabled = false
            //setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context)
        }

        b.title.setText(getOwnerTypeText(noteOwner))
        b.title.setCompoundDrawables(
            getOwnerTypeImage(noteOwner).resolveDrawable(activity),
            null,
            null,
            null,
        )
    }

    fun getOwnerTypeText(owner: Noteable) = when (owner.getNoteType()) {
        Note.OwnerType.ANNOUNCEMENT -> R.string.notes_type_announcement
        Note.OwnerType.ATTENDANCE -> R.string.notes_type_attendance
        Note.OwnerType.BEHAVIOR -> R.string.notes_type_behavior
        Note.OwnerType.DAY -> R.string.notes_type_day
        Note.OwnerType.EVENT -> R.string.notes_type_event
        Note.OwnerType.EVENT_SUBJECT -> TODO()
        Note.OwnerType.GRADE -> R.string.notes_type_grade
        Note.OwnerType.LESSON -> R.string.notes_type_lesson
        Note.OwnerType.LESSON_SUBJECT -> TODO()
        Note.OwnerType.MESSAGE -> R.string.notes_type_message
    }

    fun getOwnerTypeImage(owner: Noteable) = when (owner.getNoteType()) {
        Note.OwnerType.ANNOUNCEMENT -> R.drawable.ic_announcement
        Note.OwnerType.ATTENDANCE -> R.drawable.ic_attendance
        Note.OwnerType.BEHAVIOR -> R.drawable.ic_behavior
        Note.OwnerType.DAY -> R.drawable.ic_calendar_day
        Note.OwnerType.EVENT -> R.drawable.ic_calendar_event
        Note.OwnerType.EVENT_SUBJECT -> TODO()
        Note.OwnerType.GRADE -> R.drawable.ic_grade
        Note.OwnerType.LESSON -> R.drawable.ic_timetable
        Note.OwnerType.LESSON_SUBJECT -> TODO()
        Note.OwnerType.MESSAGE -> R.drawable.ic_message
    }
}

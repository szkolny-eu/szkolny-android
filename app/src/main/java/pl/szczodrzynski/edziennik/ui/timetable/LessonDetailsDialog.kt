/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-11.
 */

package pl.szczodrzynski.edziennik.ui.timetable

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.core.manager.NoteManager
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.db.full.AttendanceFull
import pl.szczodrzynski.edziennik.data.db.full.LessonFull
import pl.szczodrzynski.edziennik.databinding.DialogLessonDetailsBinding
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ext.setText
import pl.szczodrzynski.edziennik.ext.toDrawable
import pl.szczodrzynski.edziennik.ui.attendance.AttendanceDetailsDialog
import pl.szczodrzynski.edziennik.ui.base.dialog.BindingDialog
import pl.szczodrzynski.edziennik.ui.event.EventDetailsDialog
import pl.szczodrzynski.edziennik.ui.event.EventListAdapter
import pl.szczodrzynski.edziennik.ui.event.EventManualDialog
import pl.szczodrzynski.edziennik.ui.notes.setupNotesButton
import pl.szczodrzynski.edziennik.utils.BetterLink
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Week

class LessonDetailsDialog(
    activity: AppCompatActivity,
    private val lesson: LessonFull,
    private val attendance: AttendanceFull? = null,
    private val showNotes: Boolean = true,
) : BindingDialog<DialogLessonDetailsBinding>(activity) {

    override fun getTitleRes(): Int? = null
    override fun inflate(layoutInflater: LayoutInflater) =
        DialogLessonDetailsBinding.inflate(layoutInflater)

    // TODO put the "add" button in layout for other dialogs as well
    override fun getPositiveButtonText() = R.string.close

    private lateinit var adapter: EventListAdapter
    private val manager
        get() = app.timetableManager
    private val attendanceManager
        get() = app.attendanceManager

    fun openAddEventDialog(): Boolean {
        EventManualDialog(
            activity,
            lesson.profileId,
            defaultLesson = lesson,
        ).show()
        return NO_DISMISS
    }

    override suspend fun onShow() {
        if (App.devMode)
            b.lessonId.visibility = View.VISIBLE

        b.lesson = lesson
        val lessonDate = lesson.displayDate ?: return
        val lessonTime = lesson.displayStartTime ?: return
        b.lessonDate.text =
            Week.getFullDayName(lessonDate.weekDay) + ", " + lessonDate.formattedString

        b.annotationVisible = manager.getAnnotation(activity, lesson, b.annotation)

        if (lesson.type >= Lesson.TYPE_SHIFTED_SOURCE) {
            b.shiftedLayout.visibility = View.VISIBLE
            var otherLessonDate: Date? = null
            when (lesson.type) {
                Lesson.TYPE_SHIFTED_SOURCE -> {
                    otherLessonDate = lesson.date
                    when {
                        lesson.date != lesson.oldDate -> b.shiftedText.setText(
                            R.string.timetable_lesson_shifted_other_day,
                            lesson.date?.stringY_m_d ?: "?",
                            lesson.startTime?.stringHM ?: "?"
                        )
                        lesson.startTime != lesson.oldStartTime -> b.shiftedText.setText(
                            R.string.timetable_lesson_shifted_same_day,
                            lesson.startTime?.stringHM ?: "?"
                        )
                        else -> b.shiftedText.setText(R.string.timetable_lesson_shifted)
                    }
                }
                Lesson.TYPE_SHIFTED_TARGET -> {
                    otherLessonDate = lesson.oldDate
                    when {
                        lesson.date != lesson.oldDate -> b.shiftedText.setText(
                            R.string.timetable_lesson_shifted_from_other_day,
                            lesson.oldDate?.stringY_m_d ?: "?",
                            lesson.oldStartTime?.stringHM ?: "?"
                        )
                        lesson.startTime != lesson.oldStartTime -> b.shiftedText.setText(
                            R.string.timetable_lesson_shifted_from_same_day,
                            lesson.oldStartTime?.stringHM ?: "?"
                        )
                        else -> b.shiftedText.setText(R.string.timetable_lesson_shifted_from)
                    }
                }
            }
            b.shiftedGoTo.setOnClickListener {
                dismiss()
                val dateStr = otherLessonDate?.stringY_m_d ?: return@setOnClickListener
                val intent = Intent(TimetableFragment.ACTION_SCROLL_TO_DATE).apply {
                    putExtra("timetableDate", dateStr)
                }
                activity.sendBroadcast(intent)
            }
        } else {
            b.shiftedLayout.visibility = View.GONE
        }

        if (lesson.type < Lesson.TYPE_SHIFTED_SOURCE && lesson.oldSubjectId != null && lesson.subjectId != lesson.oldSubjectId) {
            b.oldSubjectName = lesson.oldSubjectName
        }
        if (lesson.type != Lesson.TYPE_CANCELLED && lesson.displaySubjectId != null) {
            b.subjectName = lesson.subjectName
        }

        if (lesson.type < Lesson.TYPE_SHIFTED_SOURCE && lesson.oldTeacherId != null && lesson.teacherId != lesson.oldTeacherId) {
            b.oldTeacherName = lesson.oldTeacherName
        }
        if (lesson.type != Lesson.TYPE_CANCELLED && lesson.displayTeacherId != null) {
            b.teacherName = lesson.teacherName
        }

        if (lesson.oldClassroom != null && lesson.classroom != lesson.oldClassroom) {
            b.oldClassroom = lesson.oldClassroom
        }
        if (lesson.type != Lesson.TYPE_CANCELLED && lesson.displayClassroom != null) {
            b.classroom = lesson.classroom
        }

        if (lesson.type < Lesson.TYPE_SHIFTED_SOURCE && lesson.oldTeamId != null && lesson.teamId != lesson.oldTeamId) {
            b.oldTeamName = lesson.oldTeamName
        }
        if (lesson.type != Lesson.TYPE_CANCELLED && lesson.displayTeamId != null) {
            b.teamName = lesson.teamName
        }

        b.attendanceDivider.isVisible = attendance != null
        b.attendanceLayout.isVisible = attendance != null
        if (attendance != null) {
            b.attendanceView.setAttendance(attendance, app.attendanceManager, bigView = true)
            b.attendanceType.text = attendance.typeName
            b.attendanceIcon.isVisible = attendance.let {
                val icon = attendanceManager.getAttendanceIcon(it) ?: return@let false
                val color = attendanceManager.getAttendanceColor(it)
                b.attendanceIcon.setImageDrawable(icon.toDrawable(color))
                true
            }
            b.attendanceDetails.onClick {
                AttendanceDetailsDialog(
                    activity = activity,
                    attendance = attendance,
                ).show()
            }
        }

        adapter = EventListAdapter(
            activity,
            showWeekDay = false,
            showDate = false,
            showType = true,
            showTime = true,
            showSubject = true,
            markAsSeen = true,
            onEventClick = {
                EventDetailsDialog(
                    activity,
                    it,
                ).show()
            },
            onEventEditClick = {
                EventManualDialog(
                    activity,
                    it.profileId,
                    editingEvent = it,
                ).show()
            }
        )

        app.db.eventDao().getAllByDateTime(
            lesson.profileId,
            lessonDate,
            lessonTime
        ).observe(activity) { events ->
            events.forEach {
                it.filterNotes()
            }

            adapter.setAllItems(events)
            if (b.eventsView.adapter == null) {
                b.eventsView.adapter = adapter
                b.eventsView.apply {
                    isNestedScrollingEnabled = false
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(context)
                    addItemDecoration(SimpleDividerItemDecoration(context))
                }
            }
            adapter.notifyDataSetChanged()

            if (events != null && events.isNotEmpty()) {
                b.eventsView.visibility = View.VISIBLE
                b.eventsNoData.visibility = View.GONE
            } else {
                b.eventsView.visibility = View.GONE
                b.eventsNoData.visibility = View.VISIBLE
            }
        }

        lesson.displayTeacherName?.let { name ->
            lesson.displayTeacherId ?: return@let
            BetterLink.attach(
                b.teacherNameView,
                teachers = mapOf(lesson.displayTeacherId!! to name),
                onActionSelected = ::dismiss
            )
            BetterLink.attach(
                b.oldTeacherNameView,
                teachers = mapOf(lesson.displayTeacherId!! to name),
                onActionSelected = ::dismiss
            )
        }

        b.addEventButton.onClick { openAddEventDialog() }
        b.notesButton.isVisible = showNotes
        b.notesButton.setupNotesButton(
            activity = activity,
            owner = lesson,
        )
        b.legend.isVisible = showNotes
        if (showNotes)
            NoteManager.setLegendText(lesson, b.legend)
    }
}

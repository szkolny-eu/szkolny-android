/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-11.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.timetable

import android.content.Intent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.db.full.AttendanceFull
import pl.szczodrzynski.edziennik.data.db.full.LessonFull
import pl.szczodrzynski.edziennik.databinding.DialogLessonDetailsBinding
import pl.szczodrzynski.edziennik.onClick
import pl.szczodrzynski.edziennik.setText
import pl.szczodrzynski.edziennik.ui.dialogs.event.EventDetailsDialog
import pl.szczodrzynski.edziennik.ui.dialogs.event.EventListAdapter
import pl.szczodrzynski.edziennik.ui.dialogs.event.EventManualDialog
import pl.szczodrzynski.edziennik.ui.modules.attendance.AttendanceDetailsDialog
import pl.szczodrzynski.edziennik.ui.modules.timetable.TimetableFragment
import pl.szczodrzynski.edziennik.utils.BetterLink
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Week
import kotlin.coroutines.CoroutineContext

class LessonDetailsDialog(
        val activity: AppCompatActivity,
        val lesson: LessonFull,
        val attendance: AttendanceFull? = null,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {
    companion object {
        private const val TAG = "LessonDetailsDialog"
    }

    private lateinit var app: App
    private lateinit var b: DialogLessonDetailsBinding
    private lateinit var dialog: AlertDialog

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private lateinit var adapter: EventListAdapter
    private val manager
        get() = app.timetableManager
    private val attendanceManager
        get() = app.attendanceManager

    init { run {
        if (activity.isFinishing)
            return@run
        onShowListener?.invoke(TAG)
        app = activity.applicationContext as App
        b = DialogLessonDetailsBinding.inflate(activity.layoutInflater)
        dialog = MaterialAlertDialogBuilder(activity)
                .setView(b.root)
                .setPositiveButton(R.string.close) { dialog, _ ->
                    dialog.dismiss()
                }
                .setNeutralButton(R.string.add, null)
                .setOnDismissListener {
                    onDismissListener?.invoke(TAG)
                }
                .show()

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.onClick {
            EventManualDialog(
                    activity,
                    lesson.profileId,
                    defaultLesson = lesson,
                    onShowListener = onShowListener,
                    onDismissListener = onDismissListener
            )
        }

        if (App.devMode)
            b.lessonId.visibility = View.VISIBLE

        update()
    }}

    private fun update() {
        b.lesson = lesson
        val lessonDate = lesson.displayDate ?: return
        val lessonTime = lesson.displayStartTime ?: return
        b.lessonDate.text = Week.getFullDayName(lessonDate.weekDay) + ", " + lessonDate.formattedString

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
                dialog.dismiss()
                val dateStr = otherLessonDate?.stringY_m_d ?: return@setOnClickListener
                val intent = Intent(TimetableFragment.ACTION_SCROLL_TO_DATE).apply {
                    putExtra("timetableDate", dateStr)
                }
                activity.sendBroadcast(intent)
            }
        }
        else {
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
                b.attendanceIcon.setImageDrawable(
                    IconicsDrawable(activity, icon).apply {
                        colorInt = color
                        sizeDp = 24
                    }
                )
                true
            }
            b.attendanceDetails.onClick {
                AttendanceDetailsDialog(activity, attendance, onShowListener, onDismissListener)
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
                onItemClick = {
                    EventDetailsDialog(
                            activity,
                            it,
                            onShowListener = onShowListener,
                            onDismissListener = onDismissListener
                    )
                },
                onEventEditClick = {
                    EventManualDialog(
                            activity,
                            it.profileId,
                            editingEvent = it,
                            onShowListener = onShowListener,
                            onDismissListener = onDismissListener
                    )
                }
        )

        app.db.eventDao().getAllByDateTime(lesson.profileId, lessonDate, lessonTime).observe(activity, Observer { events ->
            adapter.items = events
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
        })

        lesson.displayTeacherName?.let { name ->
            lesson.displayTeacherId ?: return@let
            BetterLink.attach(
                b.teacherNameView,
                teachers = mapOf(lesson.displayTeacherId!! to name),
                onActionSelected = dialog::dismiss
            )
            BetterLink.attach(
                b.oldTeacherNameView,
                teachers = mapOf(lesson.displayTeacherId!! to name),
                onActionSelected = dialog::dismiss
            )
        }
    }
}

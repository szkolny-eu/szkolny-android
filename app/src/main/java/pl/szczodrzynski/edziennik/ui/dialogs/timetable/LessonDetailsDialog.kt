/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-11.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.timetable

import android.content.Intent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.modules.timetable.Lesson
import pl.szczodrzynski.edziennik.data.db.modules.timetable.LessonFull
import pl.szczodrzynski.edziennik.databinding.DialogLessonDetailsBinding
import pl.szczodrzynski.edziennik.setText
import pl.szczodrzynski.edziennik.ui.dialogs.event.EventManualV2Dialog
import pl.szczodrzynski.edziennik.ui.modules.timetable.v2.TimetableFragment
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Week

class LessonDetailsDialog(
        val activity: AppCompatActivity,
        val lesson: LessonFull,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) {
    companion object {
        private const val TAG = "LessonDetailsDialog"
    }

    private lateinit var b: DialogLessonDetailsBinding
    private lateinit var dialog: AlertDialog

    init { run {
        if (activity.isFinishing)
            return@run
        onShowListener?.invoke(TAG)
        b = DialogLessonDetailsBinding.inflate(activity.layoutInflater)
        dialog = MaterialAlertDialogBuilder(activity)
                .setView(b.root)
                .setPositiveButton(R.string.close) { dialog, _ ->
                    dialog.dismiss()
                }
                .setNeutralButton(R.string.add) { dialog, _ ->
                    EventManualV2Dialog(
                            activity,
                            lesson.profileId,
                            lesson,
                            onShowListener = onShowListener,
                            onDismissListener = onDismissListener
                    )
                    /*MaterialAlertDialogBuilder(activity)
                            .setItems(R.array.main_menu_add_options) { dialog2, which ->
                                dialog2.dismiss()
                                EventManualDialog(activity, lesson.profileId)
                                        .show(
                                                activity.application as App,
                                                null,
                                                lesson.displayDate,
                                                lesson.displayStartTime,
                                                when (which) {
                                                    1 -> EventManualDialog.DIALOG_HOMEWORK
                                                    else -> EventManualDialog.DIALOG_EVENT
                                                }
                                        )

                            }
                            .setNegativeButton(R.string.cancel) { dialog2, _ -> dialog2.dismiss() }
                            .show()*/
                }
                .setOnDismissListener {
                    onDismissListener?.invoke(TAG)
                }
                .show()
        update()
    }}

    private fun update() {
        b.lesson = lesson
        val lessonDate = lesson.displayDate ?: return
        b.lessonDate.text = Week.getFullDayName(lessonDate.weekDay) + ", " + lessonDate.formattedString

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
    }
}

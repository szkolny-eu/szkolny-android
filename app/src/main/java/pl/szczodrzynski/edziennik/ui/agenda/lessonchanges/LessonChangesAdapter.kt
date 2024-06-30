/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-19.
 */

package pl.szczodrzynski.edziennik.ui.agenda.lessonchanges

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.db.full.LessonFull
import pl.szczodrzynski.edziennik.databinding.TimetableLessonBinding
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.utils.managers.NoteManager

class LessonChangesAdapter(
        val context: Context,
        private val showNotes: Boolean = true,
        private val onLessonClick: ((lesson: LessonFull) -> Unit)? = null
) : RecyclerView.Adapter<LessonChangesAdapter.ViewHolder>() {

    var items = listOf<LessonFull>()

    private val arrowRight = " → "
    private val bullet = " • "
    private val colorSecondary = android.R.attr.textColorSecondary.resolveAttr(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = TimetableLessonBinding.inflate(inflater, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val lesson = items[position]
        val b = holder.b

        if (onLessonClick != null) {
            b.root.onClick {
                onLessonClick.invoke(lesson)
            }
        }

        val startTime = lesson.displayStartTime ?: return
        val endTime = lesson.displayEndTime ?: return

        val timeRange = "${startTime.stringHM} - ${endTime.stringHM}".asColoredSpannable(colorSecondary)

        b.unread = false
        b.root.background = null

        b.root.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            topMargin = 16.dp
        }

        // teacher
        val teacherInfo = if (lesson.teacherId != null && lesson.teacherId == lesson.oldTeacherId)
            lesson.teacherName ?: "?"
        else
            mutableListOf<CharSequence>().apply {
                lesson.oldTeacherName?.let { add(it.asStrikethroughSpannable()) }
                lesson.teacherName?.let { add(it) }
            }.concat(arrowRight)

        // team
        val teamInfo = if (lesson.teamId != null && lesson.teamId == lesson.oldTeamId)
            lesson.teamName ?: "?"
        else
            mutableListOf<CharSequence>().apply {
                lesson.oldTeamName?.let { add(it.asStrikethroughSpannable()) }
                lesson.teamName?.let { add(it) }
            }.concat(arrowRight)

        // classroom
        val classroomInfo = if (lesson.classroom != null && lesson.classroom == lesson.oldClassroom)
            lesson.classroom ?: "?"
        else
            mutableListOf<CharSequence>().apply {
                lesson.oldClassroom?.let { add(it.asStrikethroughSpannable()) }
                lesson.classroom?.let { add(it) }
            }.concat(arrowRight)


        b.lessonNumber = lesson.displayLessonNumber
        val lessonText = lesson.getNoteSubstituteText(showNotes) ?: lesson.displaySubjectName
        b.subjectName.text = lessonText?.let {
            if (lesson.type == Lesson.TYPE_CANCELLED || lesson.type == Lesson.TYPE_SHIFTED_SOURCE)
                it.asStrikethroughSpannable().asColoredSpannable(colorSecondary)
            else
                it
        }
        b.detailsFirst.text = listOfNotEmpty(timeRange, classroomInfo).concat(bullet)
        b.detailsSecond.text = listOfNotEmpty(teacherInfo, teamInfo).concat(bullet)

        if (showNotes)
            NoteManager.prependIcon(lesson, b.subjectName)

        //lb.subjectName.typeface = Typeface.create("sans-serif-light", Typeface.BOLD)
        when (lesson.type) {
            Lesson.TYPE_NORMAL -> {
                b.annotationVisible = false
            }
            Lesson.TYPE_CANCELLED -> {
                b.annotationVisible = true
                b.annotation.setText(R.string.timetable_lesson_cancelled)
                //lb.subjectName.typeface = Typeface.DEFAULT
            }
            Lesson.TYPE_CHANGE -> {
                b.annotationVisible = true
                b.annotation.setText(R.string.timetable_lesson_change)
            }
            Lesson.TYPE_SHIFTED_SOURCE -> {
                b.annotationVisible = true
                when {
                    lesson.date != lesson.oldDate && lesson.date != null ->
                        b.annotation.setText(
                                R.string.timetable_lesson_shifted_other_day,
                                lesson.date?.stringY_m_d ?: "?",
                                lesson.startTime?.stringHM ?: ""
                        )

                    lesson.startTime != lesson.oldStartTime && lesson.startTime != null ->
                        b.annotation.setText(
                                R.string.timetable_lesson_shifted_same_day,
                                lesson.startTime?.stringHM ?: "?"
                        )

                    else -> b.annotation.setText(R.string.timetable_lesson_shifted)
                }
            }
            Lesson.TYPE_SHIFTED_TARGET -> {
                b.annotationVisible = true
                when {
                    lesson.date != lesson.oldDate && lesson.oldDate != null ->
                        b.annotation.setText(
                                R.string.timetable_lesson_shifted_from_other_day,
                                lesson.oldDate?.stringY_m_d ?: "?",
                                lesson.oldStartTime?.stringHM ?: ""
                        )

                    lesson.startTime != lesson.oldStartTime && lesson.oldStartTime != null ->
                        b.annotation.setText(
                                R.string.timetable_lesson_shifted_from_same_day,
                                lesson.oldStartTime?.stringHM ?: "?"
                        )

                    else -> b.annotation.setText(R.string.timetable_lesson_shifted_from)
                }
            }
        }
    }

    override fun getItemCount() = items.size

    class ViewHolder(val b: TimetableLessonBinding) : RecyclerView.ViewHolder(b.root)
}

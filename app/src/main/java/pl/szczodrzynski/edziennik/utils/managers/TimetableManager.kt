/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-10.
 */

package pl.szczodrzynski.edziennik.utils.managers

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.db.full.LessonFull
import pl.szczodrzynski.navlib.getColorFromAttr
import kotlin.coroutines.CoroutineContext

class TimetableManager(val app: App) : CoroutineScope {

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    fun markAsSeen(lesson: LessonFull) {
        lesson.seen = true
        if (lesson.type <= Lesson.TYPE_NORMAL)
            return
        startCoroutineTimer(500L, 0L) {
            app.db.metadataDao().setSeen(lesson.profileId, lesson, true)
        }
    }

    fun getAnnotation(context: Context, lesson: LessonFull, annotation: TextView): Boolean {
        var annotationVisible = false
        when (lesson.type) {
            Lesson.TYPE_CANCELLED -> {
                annotationVisible = true
                annotation.setText(R.string.timetable_lesson_cancelled)
                annotation.background.colorFilter = PorterDuffColorFilter(
                        getColorFromAttr(context, R.attr.timetable_lesson_cancelled_color),
                        PorterDuff.Mode.SRC_ATOP
                )
                //lb.subjectName.typeface = Typeface.DEFAULT
            }
            Lesson.TYPE_CHANGE -> {
                annotationVisible = true
                when {
                    lesson.subjectId != lesson.oldSubjectId && lesson.teacherId != lesson.oldTeacherId
                            && lesson.oldSubjectName != null && lesson.oldTeacherName != null ->
                        annotation.setText(
                                R.string.timetable_lesson_change_format,
                                "${lesson.oldSubjectName ?: "?"}, ${lesson.oldTeacherName ?: "?"}"
                        )

                    lesson.subjectId != lesson.oldSubjectId && lesson.oldSubjectName != null ->
                        annotation.setText(
                                R.string.timetable_lesson_change_format,
                                lesson.oldSubjectName ?: "?"
                        )

                    lesson.teacherId != lesson.oldTeacherId && lesson.oldTeacherName != null ->
                        annotation.setText(
                                R.string.timetable_lesson_change_format,
                                lesson.oldTeacherName ?: "?"
                        )
                    else -> annotation.setText(R.string.timetable_lesson_change)
                }

                annotation.background.colorFilter = PorterDuffColorFilter(
                        getColorFromAttr(context, R.attr.timetable_lesson_change_color),
                        PorterDuff.Mode.SRC_ATOP
                )
            }
            Lesson.TYPE_SHIFTED_SOURCE -> {
                annotationVisible = true
                when {
                    lesson.date != lesson.oldDate && lesson.date != null ->
                        annotation.setText(
                                R.string.timetable_lesson_shifted_other_day,
                                lesson.date?.stringY_m_d ?: "?",
                                lesson.startTime?.stringHM ?: ""
                        )

                    lesson.startTime != lesson.oldStartTime && lesson.startTime != null ->
                        annotation.setText(
                                R.string.timetable_lesson_shifted_same_day,
                                lesson.startTime?.stringHM ?: "?"
                        )

                    else -> annotation.setText(R.string.timetable_lesson_shifted)
                }

                annotation.background.setTintColor(R.attr.timetable_lesson_shifted_source_color.resolveAttr(context))
            }
            Lesson.TYPE_SHIFTED_TARGET -> {
                annotationVisible = true
                when {
                    lesson.date != lesson.oldDate && lesson.oldDate != null ->
                        annotation.setText(
                                R.string.timetable_lesson_shifted_from_other_day,
                                lesson.oldDate?.stringY_m_d ?: "?",
                                lesson.oldStartTime?.stringHM ?: ""
                        )

                    lesson.startTime != lesson.oldStartTime && lesson.oldStartTime != null ->
                        annotation.setText(
                                R.string.timetable_lesson_shifted_from_same_day,
                                lesson.oldStartTime?.stringHM ?: "?"
                        )

                    else -> annotation.setText(R.string.timetable_lesson_shifted_from)
                }

                annotation.background.colorFilter = PorterDuffColorFilter(
                        getColorFromAttr(context, R.attr.timetable_lesson_shifted_target_color),
                        PorterDuff.Mode.SRC_ATOP
                )
            }
        }
        return annotationVisible
    }
}

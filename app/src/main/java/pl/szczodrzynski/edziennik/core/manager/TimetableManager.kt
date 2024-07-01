/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-10.
 */

package pl.szczodrzynski.edziennik.core.manager

import android.content.Context
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.db.full.LessonFull
import pl.szczodrzynski.edziennik.ext.setText
import pl.szczodrzynski.edziennik.ext.startCoroutineTimer
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
                //lb.subjectName.typeface = Typeface.DEFAULT
            }
            Lesson.TYPE_CHANGE -> {
                annotationVisible = true
                annotation.setText(R.string.timetable_lesson_change)
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
            }
        }
        return annotationVisible
    }
}

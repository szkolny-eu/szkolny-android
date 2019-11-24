package pl.szczodrzynski.edziennik.data.db.modules.timetable

import android.content.Context
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class LessonFull(profileId: Int, id: Long) : Lesson(profileId, id) {
    var subjectName: String? = null
    var teacherName: String? = null
    var teamName: String? = null
    var oldSubjectName: String? = null
    var oldTeacherName: String? = null
    var oldTeamName: String? = null

    val displayDate: Date?
        get() {
            if (type == TYPE_SHIFTED_SOURCE)
                return oldDate
            return date ?: oldDate
        }

    val displayLessonNumber: Int?
        get() {
            if (type == TYPE_SHIFTED_SOURCE)
                return oldLessonNumber
            return lessonNumber ?: oldLessonNumber
        }

    val displayStartTime: Time?
        get() {
            if (type == TYPE_SHIFTED_SOURCE)
                return oldStartTime
            return startTime ?: oldStartTime
        }

    val displayEndTime: Time?
        get() {
            if (type == TYPE_SHIFTED_SOURCE)
                return oldEndTime
            return endTime ?: oldEndTime
        }

    val displaySubjectName: String?
        get() {
            if (type == TYPE_SHIFTED_SOURCE)
                return oldSubjectName
            return subjectName ?: oldSubjectName
        }

    val displayTeacherName: String?
        get() {
            if (type == TYPE_SHIFTED_SOURCE)
                return oldTeacherName
            return teacherName ?: oldTeacherName
        }

    val displayTeamName: String?
        get() {
            if (type == TYPE_SHIFTED_SOURCE)
                return oldTeamName
            return teamName ?: oldTeamName
        }

    val displayClassroom: String?
        get() {
            if (type == TYPE_SHIFTED_SOURCE)
                return oldClassroom
            return classroom ?: oldClassroom
        }

    val displayTeamId: Long?
        get() {
            if (type == TYPE_SHIFTED_SOURCE)
                return oldTeamId
            return teamId ?: oldTeamId
        }

    val displaySubjectId: Long?
        get() {
            if (type == TYPE_SHIFTED_SOURCE)
                return oldSubjectId
            return subjectId ?: oldSubjectId
        }

    val displayTeacherId: Long?
        get() {
            if (type == TYPE_SHIFTED_SOURCE)
                return oldTeacherId
            return teacherId ?: oldTeacherId
        }

    fun getDisplayChangeType(context: Context): String {
        return context.getString(when (type) {
            TYPE_CHANGE -> R.string.lesson_change
            TYPE_CANCELLED -> R.string.lesson_cancelled
            TYPE_SHIFTED_TARGET, TYPE_SHIFTED_SOURCE -> R.string.lesson_shifted
            else -> R.string.lesson_timetable_change
        })
    }

    val changeSubjectName: String
        get() {
            val first = when (type) {
                TYPE_CHANGE, TYPE_CANCELLED, TYPE_SHIFTED_SOURCE -> oldSubjectName
                else -> subjectName
            }

            val second = when (type) {
                TYPE_CHANGE -> subjectName
                else -> null
            }

            return when (second) {
                null -> first ?: ""
                else -> "$first -> $second"
            }
        }

    // metadata
    var seen: Boolean = false
    var notified: Boolean = false
    var addedDate: Long = 0
}

/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-25.
 */
package pl.szczodrzynski.edziennik.data.db.full

import android.content.Context
import androidx.room.Relation
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.db.entity.Note
import pl.szczodrzynski.edziennik.data.db.entity.Noteable
import pl.szczodrzynski.edziennik.ext.takePositive
import pl.szczodrzynski.edziennik.utils.models.Time

class LessonFull(
        profileId: Int, id: Long
) : Lesson(
        profileId, id
), Noteable {
    var subjectName: String? = null
    var teacherName: String? = null
    var teamName: String? = null
    var oldSubjectName: String? = null
    var oldTeacherName: String? = null
    var oldTeamName: String? = null

    val displayLessonNumber: Int?
        get() {
            if (type == TYPE_SHIFTED_SOURCE)
                return oldLessonNumber
            return lessonNumber ?: oldLessonNumber
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

    private fun changeText(actual: String?, old: String?): String {
        val first = when (type) {
            TYPE_CHANGE, TYPE_CANCELLED, TYPE_SHIFTED_SOURCE -> old
            else -> actual
        }

        val second = when (type) {
            TYPE_CHANGE -> actual
            else -> null
        }

        return when (second) {
            null -> first.orEmpty()
            first -> second
            else -> if (first != null) "$first -> $second" else second.orEmpty()
        }
    }

    val changeSubjectName: String
        get() = changeText(subjectName, oldSubjectName)

    val isSubjectNameChanged: Boolean
        get() = type == TYPE_CHANGE && subjectName != oldSubjectName


    val changeTeacherName: String
        get() = changeText(teacherName, oldTeacherName)

    val isTeacherNameChanged: Boolean
        get() = type == TYPE_CHANGE && teacherName != oldTeacherName


    val changeClassroom: String
        get() = changeText(classroom, oldClassroom)

    val isClassroomChanged: Boolean
        get() = type == TYPE_CHANGE && classroom != oldClassroom

    // metadata
    var seen: Boolean = false
    var notified: Boolean = false

    @Relation(parentColumn = "ownerId", entityColumn = "noteOwnerId", entity = Note::class)
    override lateinit var notes: MutableList<Note>
    override fun getNoteType() = Note.OwnerType.LESSON
    override fun getNoteOwnerProfileId() = profileId
    override fun getNoteOwnerId() = ownerId
    override fun getNoteShareTeamId() = teamId.takePositive()
}

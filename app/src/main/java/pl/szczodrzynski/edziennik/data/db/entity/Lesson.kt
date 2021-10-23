/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-25.
 */
package pl.szczodrzynski.edziennik.data.db.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

@Entity(tableName = "timetable",
        primaryKeys = ["profileId", "id"],
        indices = [
            Index(value = ["profileId", "type", "date"]),
            Index(value = ["profileId", "type", "oldDate"])
        ])
open class Lesson(
        val profileId: Int,
        var id: Long
) : Keepable() {
    companion object {
        const val TYPE_NO_LESSONS = -1
        const val TYPE_NORMAL = 0
        const val TYPE_CANCELLED = 1
        const val TYPE_CHANGE = 2
        const val TYPE_SHIFTED_SOURCE = 3 /* source lesson */
        const val TYPE_SHIFTED_TARGET = 4 /* target lesson */
    }

    var type: Int = TYPE_NORMAL

    var date: Date? = null
    var lessonNumber: Int? = null
    var startTime: Time? = null
    var endTime: Time? = null
    var subjectId: Long? = null
    var teacherId: Long? = null
    var teamId: Long? = null
    var classroom: String? = null

    var oldDate: Date? = null
    var oldLessonNumber: Int? = null
    var oldStartTime: Time? = null
    var oldEndTime: Time? = null
    var oldSubjectId: Long? = null
    var oldTeacherId: Long? = null
    var oldTeamId: Long? = null
    var oldClassroom: String? = null

    var isExtra: Boolean = false

    val displayDate: Date?
        get() {
            if (type == TYPE_SHIFTED_SOURCE)
                return oldDate
            return date ?: oldDate
        }

    val displayStartTime: Time?
        get() {
            if (type == TYPE_SHIFTED_SOURCE)
                return oldStartTime
            return startTime ?: oldStartTime
        }

    val isCancelled
        get() = type == TYPE_CANCELLED || type == TYPE_SHIFTED_SOURCE
    val isChange
        get() = type == TYPE_CHANGE || type == TYPE_SHIFTED_TARGET

    fun buildId(): Long = (displayDate?.combineWith(displayStartTime) ?: 0L) / 6L * 10L + (hashCode() and 0xFFFF)

    @Ignore
    var showAsUnseen = false

    override fun toString(): String {
        return "Lesson(profileId=$profileId, " +
                "id=$id, " +
                "type=$type, " +
                "date=$date, " +
                "lessonNumber=$lessonNumber, " +
                "startTime=$startTime, " +
                "endTime=$endTime, " +
                "subjectId=$subjectId, " +
                "teacherId=$teacherId, " +
                "teamId=$teamId, " +
                "classroom=$classroom, " +
                "oldDate=$oldDate, " +
                "oldLessonNumber=$oldLessonNumber, " +
                "oldStartTime=$oldStartTime, " +
                "oldEndTime=$oldEndTime, " +
                "oldSubjectId=$oldSubjectId, " +
                "oldTeacherId=$oldTeacherId, " +
                "oldTeamId=$oldTeamId, " +
                "oldClassroom=$oldClassroom)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Lesson) return false

        if (profileId != other.profileId) return false
        if (id != other.id) return false
        if (type != other.type) return false
        if (date != other.date) return false
        if (lessonNumber != other.lessonNumber) return false
        if (startTime != other.startTime) return false
        if (endTime != other.endTime) return false
        if (subjectId != other.subjectId) return false
        if (teacherId != other.teacherId) return false
        if (teamId != other.teamId) return false
        if (classroom != other.classroom) return false
        if (oldDate != other.oldDate) return false
        if (oldLessonNumber != other.oldLessonNumber) return false
        if (oldStartTime != other.oldStartTime) return false
        if (oldEndTime != other.oldEndTime) return false
        if (oldSubjectId != other.oldSubjectId) return false
        if (oldTeacherId != other.oldTeacherId) return false
        if (oldTeamId != other.oldTeamId) return false
        if (oldClassroom != other.oldClassroom) return false

        return true
    }

    override fun hashCode(): Int { // intentionally ignoring ID, display* and isExtra here
        var result = profileId
        result = 31 * result + type
        result = 31 * result + (date?.hashCode() ?: 0)
        // this creates problems in Mobidziennik with extra lessons
        // ... and is not generally useful anyway
        // result = 31 * result + (lessonNumber ?: 0)
        result = 31 * result + (startTime?.hashCode() ?: 0)
        result = 31 * result + (endTime?.hashCode() ?: 0)
        result = 31 * result + (subjectId?.hashCode() ?: 0)
        result = 31 * result + (teacherId?.hashCode() ?: 0)
        result = 31 * result + (teamId?.hashCode() ?: 0)
        result = 31 * result + (classroom?.hashCode() ?: 0)
        result = 31 * result + (oldDate?.hashCode() ?: 0)
        // result = 31 * result + (oldLessonNumber ?: 0)
        result = 31 * result + (oldStartTime?.hashCode() ?: 0)
        result = 31 * result + (oldEndTime?.hashCode() ?: 0)
        result = 31 * result + (oldSubjectId?.hashCode() ?: 0)
        result = 31 * result + (oldTeacherId?.hashCode() ?: 0)
        result = 31 * result + (oldTeamId?.hashCode() ?: 0)
        result = 31 * result + (oldClassroom?.hashCode() ?: 0)
        return result
    }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-24.
 */
package pl.szczodrzynski.edziennik.data.db.full

import androidx.room.Relation
import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.data.db.entity.Note
import pl.szczodrzynski.edziennik.data.db.entity.Noteable
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class AttendanceFull(
        profileId: Int, id: Long,
        baseType: Int, typeName: String, typeShort: String, typeSymbol: String, typeColor: Int?,
        date: Date, startTime: Time, semester: Int,
        teacherId: Long, subjectId: Long, addedDate: Long = System.currentTimeMillis()
) : Attendance(
        profileId, id,
        baseType, typeName, typeShort, typeSymbol, typeColor,
        date, startTime, semester,
        teacherId, subjectId, addedDate
), Noteable {
    var teacherName: String? = null
    var subjectLongName: String? = null
    var subjectShortName: String? = null

    // metadata
    var seen = false
        get() = field || baseType == TYPE_PRESENT
    var notified = false

    @Relation(parentColumn = "attendanceId", entityColumn = "noteOwnerId", entity = Note::class)
    override lateinit var notes: MutableList<Note>
    override fun getNoteType() = Note.OwnerType.ATTENDANCE
}

/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-24.
 */
package pl.szczodrzynski.edziennik.data.db.full

import androidx.room.Relation
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.entity.Note
import pl.szczodrzynski.edziennik.data.db.entity.Noteable

class GradeFull(
        profileId: Int, id: Long, name: String, type: Int,
        value: Float, weight: Float, color: Int,
        category: String?, description: String?, comment: String?,
        semester: Int, teacherId: Long, subjectId: Long, addedDate: Long = System.currentTimeMillis()
) : Grade(
        profileId, id, name, type,
        value, weight, color,
        category, description, comment,
        semester, teacherId, subjectId, addedDate
), Noteable {
    var teacherName: String? = null
    var subjectLongName: String? = null
    var subjectShortName: String? = null

    // metadata
    var seen = false
    var notified = false

    @Relation(parentColumn = "gradeId", entityColumn = "noteOwnerId", entity = Note::class)
    override lateinit var notes: MutableList<Note>
    override fun getNoteType() = Note.OwnerType.GRADE
}

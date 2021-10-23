/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-25.
 */
package pl.szczodrzynski.edziennik.data.db.full

import androidx.room.Relation
import pl.szczodrzynski.edziennik.data.db.entity.Note
import pl.szczodrzynski.edziennik.data.db.entity.Noteable
import pl.szczodrzynski.edziennik.data.db.entity.Notice

class NoticeFull(
        profileId: Int, id: Long, type: Int, semester: Int,
        text: String, category: String?, points: Float?,
        teacherId: Long, addedDate: Long = System.currentTimeMillis()
) : Notice(
        profileId, id, type, semester,
        text, category, points,
        teacherId, addedDate
), Noteable {
    var teacherName: String? = null

    // metadata
    var seen = false
    var notified = false

    @Relation(parentColumn = "noticeId", entityColumn = "noteOwnerId", entity = Note::class)
    override lateinit var notes: MutableList<Note>
    override fun getNoteType() = Note.OwnerType.BEHAVIOR
    override fun getNoteOwnerProfileId() = profileId
    override fun getNoteOwnerId() = id
}

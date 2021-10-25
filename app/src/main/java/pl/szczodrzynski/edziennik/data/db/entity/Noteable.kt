/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.data.db.entity

interface Noteable {

    fun getNoteType(): Note.OwnerType
    fun getNoteOwnerProfileId(): Int
    fun getNoteOwnerId(): Long

    var notes: MutableList<Note>

    fun filterNotes() {
        val type = getNoteType()
        val profileId = getNoteOwnerProfileId()
        notes.removeAll {
            it.profileId != profileId || it.ownerType != type
        }
    }

    fun hasNotes() = notes.isNotEmpty()

    fun getNoteSubstituteText(showNotes: Boolean): CharSequence? {
        if (!showNotes)
            return null
        val note = notes.firstOrNull {
            it.replacesOriginal
        }
        return note?.topicHtml ?: note?.bodyHtml
    }
}

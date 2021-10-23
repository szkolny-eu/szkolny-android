/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.data.db.entity

interface Noteable {

    fun getNoteType(): Note.OwnerType

    var notes: MutableList<Note>

    fun filterNotes(profileId: Int) {
        val type = getNoteType()
        notes.removeAll {
            it.profileId != profileId || it.ownerType != type
        }
    }

    fun hasNotes() = notes.isNotEmpty()

    fun getNoteSubstituteText(): CharSequence? {
        val note = notes.firstOrNull {
            it.replacesOriginal
        }
        return note?.topicHtml ?: note?.bodyHtml
    }
}

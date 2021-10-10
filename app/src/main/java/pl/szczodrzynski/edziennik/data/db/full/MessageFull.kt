/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-25.
 */
package pl.szczodrzynski.edziennik.data.db.full

import androidx.room.Ignore
import androidx.room.Relation
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.MessageRecipient
import pl.szczodrzynski.edziennik.ui.modules.search.Searchable

class MessageFull(
        profileId: Int, id: Long, type: Int,
        subject: String, body: String?, senderId: Long?,
        addedDate: Long = System.currentTimeMillis()
) : Message(
        profileId, id, type,
        subject, body, senderId,
        addedDate
), Searchable<MessageFull> {
    var senderName: String? = null
    @Relation(parentColumn = "messageId", entityColumn = "messageId", entity = MessageRecipient::class)
    var recipients: MutableList<MessageRecipientFull>? = null

    fun addRecipient(recipient: MessageRecipientFull): MessageFull {
        if (recipients == null) recipients = mutableListOf()
        recipients?.add(recipient)
        return this
    }

    @Ignore
    override var searchPriority = 0

    @Ignore
    override var searchHighlightText: CharSequence? = null

    @delegate:Ignore
    override val searchKeywords by lazy {
        listOf(
            when {
                isSent -> recipients?.map { it.fullName } ?: listOf()
                else -> listOf(senderName)
            },
            listOf(subject),
            listOf(body)
        )
    }

    override fun compareTo(other: Searchable<*>): Int {
        if (other !is MessageFull)
            return 0
        return when {
            // ascending sorting
            searchPriority > other.searchPriority -> 1
            searchPriority < other.searchPriority -> -1
            // descending sorting (1. true, 2. false)
            isStarred && !other.isStarred -> -1
            !isStarred && other.isStarred -> 1
            // descending sorting
            addedDate > other.addedDate -> -1
            addedDate < other.addedDate -> 1
            else -> 0
        }
    }

    @Ignore
    var readByEveryone = true

    // metadata
    var seen = false
    var notified = false
}

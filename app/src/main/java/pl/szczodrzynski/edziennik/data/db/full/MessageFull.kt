/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-25.
 */
package pl.szczodrzynski.edziennik.data.db.full

import androidx.core.text.HtmlCompat
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

    @delegate:Ignore
    @delegate:Transient
    val bodyHtml by lazy {
        body?.let {
            HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    }


    @Ignore
    @Transient
    override var searchPriority = 0

    @Ignore
    @Transient
    override var searchHighlightText: String? = null

    @delegate:Ignore
    @delegate:Transient
    override val searchKeywords by lazy {
        listOf(
            when {
                isSent -> recipients?.map { it.fullName }
                else -> listOf(senderName)
            },
            listOf(subject),
            listOf(bodyHtml?.toString()),
            attachmentNames,
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
    @Transient
    var readByEveryone = true

    // metadata
    var seen = false
    var notified = false
}

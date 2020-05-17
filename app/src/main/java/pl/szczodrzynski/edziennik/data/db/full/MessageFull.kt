/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-25.
 */
package pl.szczodrzynski.edziennik.data.db.full

import androidx.room.Ignore
import androidx.room.Relation
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.MessageRecipient

class MessageFull(
        profileId: Int, id: Long, type: Int,
        subject: String, body: String?, senderId: Long?,
        addedDate: Long = System.currentTimeMillis()
) : Message(
        profileId, id, type,
        subject, body, senderId,
        addedDate
) {
    var senderName: String? = null
    @Relation(parentColumn = "messageId", entityColumn = "messageId", entity = MessageRecipient::class)
    var recipients: MutableList<MessageRecipientFull>? = null

    fun addRecipient(recipient: MessageRecipientFull): MessageFull {
        if (recipients == null) recipients = mutableListOf()
        recipients?.add(recipient)
        return this
    }

    @Ignore
    var filterWeight = 0
    @Ignore
    var searchHighlightText: String? = null

    // metadata
    var seen = false
    var notified = false
}

/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-24
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.messages

import pl.szczodrzynski.edziennik.DAY
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_MESSAGES
import pl.szczodrzynski.edziennik.data.api.ERROR_NOT_IMPLEMENTED
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_MESSAGES_RECEIVED
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_MESSAGES_SENT
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusMessages
import pl.szczodrzynski.edziennik.data.db.entity.*
import pl.szczodrzynski.edziennik.data.db.entity.Message.Companion.TYPE_RECEIVED
import pl.szczodrzynski.edziennik.fixName
import pl.szczodrzynski.edziennik.singleOrNull
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.edziennik.utils.models.Date

class LibrusMessagesGetList(override val data: DataLibrus,
                            override val lastSync: Long?,
                            private val type: Int = TYPE_RECEIVED,
                            archived: Boolean = false,
                            val onSuccess: (endpointId: Int) -> Unit
) : LibrusMessages(data, lastSync) {
    companion object {
        const val TAG = "LibrusMessagesGetList"
    }

    init {
        val endpoint = when (type) {
            TYPE_RECEIVED -> "Inbox/action/GetList"
            Message.TYPE_SENT -> "Outbox/action/GetList"
            else -> null
        }
        val endpointId = when (type) {
            TYPE_RECEIVED -> ENDPOINT_LIBRUS_MESSAGES_RECEIVED
            else -> ENDPOINT_LIBRUS_MESSAGES_SENT
        }

        if (endpoint != null) {
            messagesGet(TAG, endpoint, parameters = mapOf(
                    "archive" to if (archived) 1 else 0
            )) { doc ->
                doc.select("GetList data").firstOrNull()?.children()?.forEach { element ->
                    val id = element.select("messageId").text().toLong()
                    val subject = element.select("topic").text().trim()
                    val readDateText = element.select("readDate").text().trim()
                    val readDate = when (readDateText.isNotBlank()) {
                        true -> Date.fromIso(readDateText)
                        else -> 0
                    }
                    val sentDate = Date.fromIso(element.select("sendDate").text().trim())

                    val recipientFirstName = element.select(when (type) {
                        TYPE_RECEIVED -> "senderFirstName"
                        else -> "receiverFirstName"
                    }).text().fixName()

                    val recipientLastName = element.select(when (type) {
                        TYPE_RECEIVED -> "senderLastName"
                        else -> "receiverLastName"
                    }).text().fixName()

                    val recipientId = data.teacherList.singleOrNull {
                        it.name == recipientFirstName && it.surname == recipientLastName
                    }?.id ?: {
                        val teacherObject = Teacher(
                                profileId,
                                -1 * Utils.crc16("$recipientFirstName $recipientLastName".toByteArray()).toLong(),
                                recipientFirstName,
                                recipientLastName
                        )
                        data.teacherList.put(teacherObject.id, teacherObject)
                        teacherObject.id
                    }.invoke()

                    val senderId = when (type) {
                        TYPE_RECEIVED -> recipientId
                        else -> null
                    }

                    val receiverId = when (type) {
                        TYPE_RECEIVED -> -1
                        else -> recipientId
                    }

                    val notified = when (type) {
                        Message.TYPE_SENT -> true
                        else -> readDate > 0 || profile?.empty ?: false
                    }

                    val messageObject = Message(
                            profileId = profileId,
                            id = id,
                            type = type,
                            subject = subject,
                            body = null,
                            senderId = senderId
                    )

                    val messageRecipientObject = MessageRecipient(
                            profileId,
                            receiverId,
                            -1,
                            readDate,
                            id
                    )

                    element.select("isAnyFileAttached")?.text()?.let {
                        if (it == "1")
                            messageObject.hasAttachments = true
                    }

                    data.messageList.add(messageObject)
                    data.messageRecipientList.add(messageRecipientObject)
                    data.setSeenMetadataList.add(Metadata(
                            profileId,
                            Metadata.TYPE_MESSAGE,
                            id,
                            notified,
                            notified,
                            sentDate
                    ))
                }

                when (type) {
                    TYPE_RECEIVED -> data.setSyncNext(ENDPOINT_LIBRUS_MESSAGES_RECEIVED, SYNC_ALWAYS)
                    Message.TYPE_SENT -> data.setSyncNext(ENDPOINT_LIBRUS_MESSAGES_SENT, DAY, DRAWER_ITEM_MESSAGES)
                }
                onSuccess(endpointId)
            }
        } else {
            data.error(TAG, ERROR_NOT_IMPLEMENTED)
            onSuccess(endpointId)
        }
    }
}

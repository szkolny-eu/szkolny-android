/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-24
 */

package pl.szczodrzynski.edziennik.api.v2.edziennik.librus.data.messages

import pl.szczodrzynski.edziennik.DAY
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_MESSAGES
import pl.szczodrzynski.edziennik.api.v2.ERROR_NOT_IMPLEMENTED
import pl.szczodrzynski.edziennik.api.v2.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.edziennik.librus.ENDPOINT_LIBRUS_MESSAGES_RECEIVED
import pl.szczodrzynski.edziennik.api.v2.edziennik.librus.ENDPOINT_LIBRUS_MESSAGES_SENT
import pl.szczodrzynski.edziennik.api.v2.edziennik.librus.data.LibrusMessages
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message.TYPE_RECEIVED
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageRecipient
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher
import pl.szczodrzynski.edziennik.singleOrNull
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.edziennik.utils.models.Date

class LibrusMessagesGetList(override val data: DataLibrus, private val type: Int = TYPE_RECEIVED,
                            archived: Boolean = false, val onSuccess: () -> Unit) : LibrusMessages(data) {
    companion object {
        const val TAG = "LibrusMessagesGetList"
    }

    init {
        val endpoint = when (type) {
            TYPE_RECEIVED -> "Inbox/action/GetList"
            Message.TYPE_SENT -> "Outbox/action/GetList"
            else -> null
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
                    }).text().trim()

                    val recipientLastName = element.select(when (type) {
                        TYPE_RECEIVED -> "senderLastName"
                        else -> "receiverLastName"
                    }).text().trim()

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
                        else -> -1
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
                            profileId,
                            id,
                            subject,
                            null,
                            type,
                            senderId,
                            -1
                    )

                    val messageRecipientObject = MessageRecipient(
                            profileId,
                            receiverId,
                            -1,
                            readDate,
                            id
                    )

                    data.messageIgnoreList.add(messageObject)
                    data.messageRecipientList.add(messageRecipientObject)
                    data.metadataList.add(Metadata(
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
                onSuccess()
            }
        } else {
            data.error(TAG, ERROR_NOT_IMPLEMENTED)
            onSuccess()
        }
    }
}

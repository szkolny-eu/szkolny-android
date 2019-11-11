/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-11
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.messages

import android.util.Base64
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusMessages
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message.TYPE_RECEIVED
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message.TYPE_SENT
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageRecipient
import pl.szczodrzynski.edziennik.utils.models.Date

class LibrusMessagesGetMessage(override val data: DataLibrus, val messageId: Long, val onSuccess: () -> Unit) : LibrusMessages(data) {
    companion object {
        const val TAG = "LibrusMessagesGetMessage"
    }

    init {
        messagesGet(TAG, "GetMessage", parameters = mapOf(
                "messageId" to messageId,
                "archive" to 0
        )) { doc ->
            val message = doc.select("response GetMessage data").first()

            val body = Base64.decode(message.select("Message").text(), Base64.DEFAULT)
                    .toString().apply {
                        replace("\n", "")
                        replace("<!\\[CDATA\\[", "")
                        replace("]]>", "")
                    }

            val messageObject = data.db.messageDao().getById(profileId, messageId).apply {
                this.body = body

                clearAttachments()
                message.select("attachments ArrayItem").forEach {
                    val attachmentId = it.select("id").text().toLong()
                    val attachmentName = it.select("filename").text()
                    addAttachment(attachmentId, attachmentName, -1)
                }
            }

            when (messageObject.type) {
                TYPE_RECEIVED -> {
                    val senderLoginId = message.select("senderId").text()
                    val readDateText = message.select("readDate").text()
                    val readDate = when (readDateText.isNotEmpty()) {
                        true -> Date.fromIso(readDateText)
                        else -> 0
                    }

                    val messageRecipientObject = MessageRecipient(
                            profileId,
                            -1,
                            -1,
                            readDate,
                            messageObject.id
                    )

                    data.messageRecipientList.add(messageRecipientObject)
                    data.db.teacherDao().updateLoginId(profileId, messageObject.senderId, senderLoginId)
                }

                TYPE_SENT -> {
                    val teachers = data.db.teacherDao().getAllNow(profileId)

                    message.select("receivers ArrayItem").forEach { receiver ->
                        val receiverFirstName = receiver.select("firstName").text()
                        val receiverLastName = receiver.select("lastName").text()
                        val receiverLoginId = receiver.select("receiverId").text()

                        val receiverId = teachers.singleOrNull {
                            it.name.equals(receiverFirstName, true) && it.surname.equals(receiverLastName, true)
                        }?.id ?: -1

                        val readDateText = message.select("readed").text()
                        val readDate = when (readDateText.isNotEmpty()) {
                            true -> Date.fromIso(readDateText)
                            else -> 0
                        }

                        val messageRecipientObject = MessageRecipient(
                                profileId,
                                -1,
                                -1,
                                readDate,
                                messageObject.id
                        )

                        data.messageRecipientList.add(messageRecipientObject)
                        data.db.teacherDao().updateLoginId(profileId, receiverId, receiverLoginId)
                    }
                }
            }

            if (!messageObject.seen) data.db.metadataDao().setSeen(profileId, messageObject, true)

            data.messageList.add(messageObject as Message)
            onSuccess()
        }
    }
}

/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-11
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.messages

import android.util.Base64
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusMessages
import pl.szczodrzynski.edziennik.data.api.events.MessageGetEvent
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message.TYPE_RECEIVED
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message.TYPE_SENT
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageFull
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageRecipientFull
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.fixName
import pl.szczodrzynski.edziennik.singleOrNull
import pl.szczodrzynski.edziennik.utils.models.Date
import java.nio.charset.Charset

class LibrusMessagesGetMessage(
        override val data: DataLibrus,
        private val messageObject: MessageFull,
        val onSuccess: () -> Unit
) : LibrusMessages(data) {
    companion object {
        const val TAG = "LibrusMessagesGetMessage"
    }

    init { data.profile?.also { profile ->
        messagesGet(TAG, "GetMessage", parameters = mapOf(
                "messageId" to messageObject.id,
                "archive" to 0
        )) { doc ->
            val message = doc.select("response GetMessage data").first()

            val body = Base64.decode(message.select("Message").text(), Base64.DEFAULT)
                    .toString(Charset.defaultCharset())
                    .replace("\n", "<br>")
                    .replace("<![CDATA[", "")
                    .replace("]]>", "")

            messageObject.apply {
                this.body = body

                clearAttachments()
                message.select("attachments ArrayItem").forEach {
                    val attachmentId = it.select("id").text().toLong()
                    val attachmentName = it.select("filename").text()
                    addAttachment(attachmentId, attachmentName, -1)
                }
            }

            val messageRecipientList = mutableListOf<MessageRecipientFull>()

            when (messageObject.type) {
                TYPE_RECEIVED -> {
                    val senderLoginId = message.select("senderId").text()
                    data.teacherList.singleOrNull { it.id == messageObject.senderId }?.loginId = senderLoginId

                    val readDateText = message.select("readDate").text()
                    val readDate = when (readDateText.isNotEmpty()) {
                        true -> Date.fromIso(readDateText)
                        else -> 0
                    }

                    val messageRecipientObject = MessageRecipientFull(
                            profileId,
                            -1,
                            -1,
                            readDate,
                            messageObject.id
                    )

                    messageRecipientObject.fullName = profile.accountNameLong ?: profile.studentNameLong

                    messageRecipientList.add(messageRecipientObject)
                }

                TYPE_SENT -> {

                    message.select("receivers ArrayItem").forEach { receiver ->
                        val receiverFirstName = receiver.select("firstName").text().fixName()
                        val receiverLastName = receiver.select("lastName").text().fixName()
                        val receiverLoginId = receiver.select("receiverId").text()

                        val teacher = data.teacherList.singleOrNull { it.name == receiverFirstName && it.surname == receiverLastName }
                        val receiverId = teacher?.id ?: -1
                        teacher?.loginId = receiverLoginId

                        val readDateText = message.select("readed").text()
                        val readDate = when (readDateText.isNotEmpty()) {
                            true -> Date.fromIso(readDateText)
                            else -> 0
                        }

                        val messageRecipientObject = MessageRecipientFull(
                                profileId,
                                receiverId,
                                -1,
                                readDate,
                                messageObject.id
                        )

                        messageRecipientObject.fullName = "$receiverFirstName $receiverLastName"

                        messageRecipientList.add(messageRecipientObject)
                    }
                }
            }

            if (!messageObject.seen) {
                data.messageMetadataList.add(Metadata(
                        messageObject.profileId,
                        Metadata.TYPE_MESSAGE,
                        messageObject.id,
                        true,
                        true,
                        messageObject.addedDate
                ))
            }

            messageObject.recipients = messageRecipientList
            data.messageRecipientList.addAll(messageRecipientList)
            data.messageList.add(messageObject)

            EventBus.getDefault().postSticky(MessageGetEvent(messageObject))
            onSuccess()
        }
    } ?: onSuccess()}
}

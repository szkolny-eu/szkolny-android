/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-11
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.messages

import android.util.Base64
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusMessages
import pl.szczodrzynski.edziennik.data.api.events.MessageGetEvent
import pl.szczodrzynski.edziennik.data.db.entity.Message.Companion.TYPE_RECEIVED
import pl.szczodrzynski.edziennik.data.db.entity.Message.Companion.TYPE_SENT
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.data.db.full.MessageRecipientFull
import pl.szczodrzynski.edziennik.fixName
import pl.szczodrzynski.edziennik.isNotNullNorEmpty
import pl.szczodrzynski.edziennik.notEmptyOrNull
import pl.szczodrzynski.edziennik.singleOrNull
import pl.szczodrzynski.edziennik.utils.models.Date
import java.nio.charset.Charset

class LibrusMessagesGetMessage(override val data: DataLibrus,
                               private val messageObject: MessageFull,
                               val onSuccess: () -> Unit
) : LibrusMessages(data, null) {
    companion object {
        const val TAG = "LibrusMessagesGetMessage"
    }

    init { data.profile?.also { profile ->
        messagesGet(TAG, "GetMessage", parameters = mapOf(
                "messageId" to messageObject.id,
                "archive" to 0
        )) { doc ->
            val message = doc.select("response GetMessage data").first() ?: return@messagesGet

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
                    val senderLoginId = message.select("senderId").text().notEmptyOrNull()
                    val senderGroupId = message.select("senderGroupId").text().toIntOrNull()
                    val userClass = message.select("userClass").text().notEmptyOrNull()
                    data.teacherList.singleOrNull { it.id == messageObject.senderId }?.apply {
                        loginId = senderLoginId
                        setTeacherType(when (senderGroupId) {
                            /* https://api.librus.pl/2.0/Messages/Role */
                            0, 1, 99 -> Teacher.TYPE_SUPER_ADMIN
                            2 -> Teacher.TYPE_SCHOOL_ADMIN
                            3 -> Teacher.TYPE_PRINCIPAL
                            4 -> Teacher.TYPE_TEACHER
                            5, 9 -> {
                                if (typeDescription == null)
                                    typeDescription = userClass
                                Teacher.TYPE_PARENT
                            }
                            7 -> Teacher.TYPE_SECRETARIAT
                            8 -> {
                                if (typeDescription == null)
                                    typeDescription = userClass
                                Teacher.TYPE_STUDENT
                            }
                            10 -> Teacher.TYPE_PEDAGOGUE
                            11 -> Teacher.TYPE_LIBRARIAN
                            12 -> Teacher.TYPE_SPECIALIST
                            21 -> {
                                typeDescription = "Jednostka Nadrzędna"
                                Teacher.TYPE_OTHER
                            }
                            50 -> {
                                typeDescription = "Jednostka Samorządu Terytorialnego"
                                Teacher.TYPE_OTHER
                            }
                            else -> Teacher.TYPE_OTHER
                        })
                    }

                    val readDateText = message.select("readDate").text()
                    val readDate = when (readDateText.isNotNullNorEmpty()) {
                        true -> Date.fromIso(readDateText)
                        else -> 0
                    }

                    val messageRecipientObject = MessageRecipientFull(
                            profileId = profileId,
                            id = -1,
                            messageId = messageObject.id,
                            readDate = readDate
                    )

                    messageRecipientObject.fullName = profile.accountName ?: profile.studentNameLong ?: ""

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
                        val readDate = when (readDateText.isNotNullNorEmpty()) {
                            true -> Date.fromIso(readDateText)
                            else -> 0
                        }

                        val messageRecipientObject = MessageRecipientFull(
                                profileId = profileId,
                                id = receiverId,
                                messageId = messageObject.id,
                                readDate = readDate
                        )

                        messageRecipientObject.fullName = "$receiverFirstName $receiverLastName"

                        messageRecipientList.add(messageRecipientObject)
                    }
                }
            }

            if (!messageObject.seen) {
                data.setSeenMetadataList.add(Metadata(
                        messageObject.profileId,
                        Metadata.TYPE_MESSAGE,
                        messageObject.id,
                        true,
                        true
                ))
            }

            messageObject.recipients = messageRecipientList
            data.messageRecipientList.addAll(messageRecipientList)

            data.messageList.add(messageObject)
            data.messageListReplace = true

            EventBus.getDefault().postSticky(MessageGetEvent(messageObject))
            onSuccess()
        }
    } ?: onSuccess()}
}

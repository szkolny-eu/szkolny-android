/*
 * Copyright (c) Kuba Szczodrzyński 2021-2-21.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe

import androidx.core.util.set
import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_MESSAGES
import pl.szczodrzynski.edziennik.data.api.VULCAN_HEBE_ENDPOINT_MESSAGEBOXES
import pl.szczodrzynski.edziennik.data.api.VULCAN_HEBE_ENDPOINT_MESSAGES
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_HEBE_MESSAGES_INBOX
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_HEBE_MESSAGES_SENT
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanHebe
import pl.szczodrzynski.edziennik.data.db.entity.*
import pl.szczodrzynski.edziennik.data.db.entity.Message.Companion.TYPE_DELETED
import pl.szczodrzynski.edziennik.data.db.entity.Message.Companion.TYPE_RECEIVED
import pl.szczodrzynski.edziennik.data.db.entity.Message.Companion.TYPE_SENT
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.utils.Utils.crc32
import pl.szczodrzynski.navlib.crc16

class VulcanHebeMessages(
    override val data: DataVulcan,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit
) : VulcanHebe(data, lastSync) {
    companion object {
        const val TAG = "VulcanHebeMessagesInbox"
    }

    private fun getPersonId(json: JsonObject): Long {
        val senderGlobalKey = json.getString("GlobalKey") ?: return -1
        /*if (senderLoginId == data.studentLoginId)
            return -1*/

        val senderName = json.getString("Name") ?: return -1
        val senderNameSplit = senderName.splitName()
        val teacher = data.teacherList.singleOrNull { it.globalKey == senderGlobalKey }
            ?: Teacher(
                profileId,
                -1 * crc16(senderName).toLong(),
                senderNameSplit.second,
                senderNameSplit.first,
                null,
                globalKey = senderGlobalKey
            ).also {
                it.setTeacherType(Teacher.TYPE_OTHER)
                data.teacherList[it.id] = it
            }
        return teacher.id
    }

    fun getMessages(messageType: Int) {
        val folder = when (messageType) {
            TYPE_RECEIVED -> 1
            TYPE_SENT -> 2
            TYPE_DELETED -> 3
            else -> 1
        }
        val endpointId = when (messageType) {
            TYPE_RECEIVED -> ENDPOINT_VULCAN_HEBE_MESSAGES_INBOX
            TYPE_SENT -> ENDPOINT_VULCAN_HEBE_MESSAGES_SENT
            else -> ENDPOINT_VULCAN_HEBE_MESSAGES_INBOX
        }
        apiGetList(
            TAG,
            VULCAN_HEBE_ENDPOINT_MESSAGEBOXES,
            lastSync = lastSync
        ) { list, _ ->
            list.forEach { message_box ->
                apiGetList(
                    TAG,
                    VULCAN_HEBE_ENDPOINT_MESSAGES,
                    HebeFilterType.BY_MESSAGEBOX,
                    message_box = message_box.getString("GlobalKey"),
                    folder = folder,
                    lastSync = 0L
                ) { list, _ ->
                    list.forEach { message ->
                        val id = message.getString("Id") ?: return@forEach
                        val subject = message.getString("Subject") ?: return@forEach
                        val body = message.getString("Content") ?: return@forEach

                        val sender = message.getJsonObject("Sender") ?: return@forEach

                        val sentDate = getDateTime(message, "DateSent")
                        val readDate = getDateTime(message, "DateRead", default = 0)

                        if (!isCurrentYear(sentDate)) return@forEach

                        val messageObject = Message(
                            profileId = profileId,
                            crc32((id + "0").toByteArray()),
                            type = messageType,
                            subject = subject,
                            body = body.replace("\n", "<br>"),
                            senderId = if (messageType == TYPE_RECEIVED) getPersonId(sender) else null,
                            addedDate = sentDate
                        )

                        val receivers = message.getJsonArray("Receiver")
                            ?.asJsonObjectList()
                            ?: return@forEach
                        val receiverReadDate =
                            if (receivers.size == 1) readDate
                            else -1

                        for (receiver in receivers) {
                            val messageRecipientObject = MessageRecipient(
                                profileId,
                                if (messageType == TYPE_SENT) getPersonId(receiver) else -1,
                                -1,
                                receiverReadDate,
                                crc32((id + "0").toByteArray()),
                            )
                            data.messageRecipientList.add(messageRecipientObject)
                        }

                        val attachments = message.getJsonArray("Attachments")
                            ?.asJsonObjectList()
                            ?: return@forEach

                        for (attachment in attachments) {
                            val fileName = attachment.getString("Name") ?: continue
                            val url = attachment.getString("Link") ?: continue
                            val attachmentName = "$fileName:$url"
                            val attachmentId = crc32(attachmentName.toByteArray())

                            messageObject.addAttachment(
                                id = attachmentId,
                                name = attachmentName,
                                size = -1
                            )
                        }

                        data.messageList.add(messageObject)
                        data.setSeenMetadataList.add(
                            Metadata(
                                profileId,
                                Metadata.TYPE_MESSAGE,
                                crc32((id + "0").toByteArray()),
                                readDate > 0 || messageType == TYPE_SENT,
                                readDate > 0 || messageType == TYPE_SENT
                            )
                        )
                    }

                    data.setSyncNext(
                        endpointId,
                        if (messageType == TYPE_RECEIVED) SYNC_ALWAYS else 1 * DAY,
                        if (messageType == TYPE_RECEIVED) null else DRAWER_ITEM_MESSAGES
                    )
                    onSuccess(endpointId)
                }
            }
        }
    }
}

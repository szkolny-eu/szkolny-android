/*
 * Copyright (c) Kuba Szczodrzyński 2021-2-21.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe

import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_MESSAGES
import pl.szczodrzynski.edziennik.data.api.VULCAN_HEBE_ENDPOINT_MESSAGES
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_HEBE_MESSAGES_INBOX
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_HEBE_MESSAGES_SENT
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanHebe
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Message.Companion.TYPE_DELETED
import pl.szczodrzynski.edziennik.data.db.entity.Message.Companion.TYPE_RECEIVED
import pl.szczodrzynski.edziennik.data.db.entity.Message.Companion.TYPE_SENT
import pl.szczodrzynski.edziennik.data.db.entity.MessageRecipient
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.utils.Utils

class VulcanHebeMessages(
    override val data: DataVulcan,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit
) : VulcanHebe(data, lastSync) {
    companion object {
        const val TAG = "VulcanHebeMessages"
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

        val messageBox = data.messageBoxKey
        if (messageBox == null) {
            onSuccess(endpointId)
            return
        }

        apiGetList(
            TAG,
            VULCAN_HEBE_ENDPOINT_MESSAGES,
            HebeFilterType.BY_MESSAGEBOX,
            messageBox = data.messageBoxKey,
            folder = folder,
            lastSync = lastSync
        ) { list, _ ->
            list.forEach { message ->
                val uuid = message.getString("Id") ?: return@forEach
                val id = Utils.crc32(uuid.toByteArray())
                val subject = message.getString("Subject") ?: return@forEach
                val body = message.getString("Content") ?: return@forEach

                val sender = message.getJsonObject("Sender") ?: return@forEach

                val sentDate = getDateTime(message, "DateSent")
                val readDate = getDateTime(message, "DateRead", default = 0)

                if (!isCurrentYear(sentDate)) return@forEach

                val senderId = if (messageType == TYPE_RECEIVED)
                    getTeacherRecipient(sender)?.id
                else
                    null

                val messageObject = Message(
                    profileId = profileId,
                    id = id,
                    type = messageType,
                    subject = subject,
                    body = body.replace("\n", "<br>"),
                    senderId = senderId,
                    addedDate = sentDate
                )

                val receivers = message.getJsonArray("Receiver")
                    ?.asJsonObjectList()
                    ?: return@forEach
                val receiverReadDate =
                    if (receivers.size == 1) readDate
                    else -1

                for (receiver in receivers) {
                    val recipientId = if (messageType == TYPE_SENT)
                        getTeacherRecipient(receiver)?.id ?: -1
                    else
                        -1

                    val messageRecipientObject = MessageRecipient(
                        profileId,
                        recipientId,
                        -1,
                        receiverReadDate,
                        id
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
                    val attachmentId = Utils.crc32(attachmentName.toByteArray())

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
                        id,
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

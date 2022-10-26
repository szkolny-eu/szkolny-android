/*
 * Copyright (c) Kuba Szczodrzyński 2021-2-22.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.ERROR_MESSAGE_NOT_SENT
import pl.szczodrzynski.edziennik.data.api.ERROR_VULCAN_HEBE_MISSING_SENDER_ENTRY
import pl.szczodrzynski.edziennik.data.api.VULCAN_HEBE_ENDPOINT_MESSAGEBOX_SEND
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanHebe
import pl.szczodrzynski.edziennik.data.api.events.MessageSentEvent
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.ext.*
import java.util.UUID

class VulcanHebeSendMessage(
    override val data: DataVulcan,
    val recipients: Set<Teacher>,
    val subject: String,
    val text: String,
    val onSuccess: () -> Unit
) : VulcanHebe(data, null) {
    companion object {
        const val TAG = "VulcanHebeSendMessage"
    }

    init {
        if (data.messageBoxKey == null || data.messageBoxName == null) {
            VulcanHebeMessageBoxes(data, 0) {
                if (data.messageBoxKey == null || data.messageBoxName == null) {
                    data.error(TAG, ERROR_VULCAN_HEBE_MISSING_SENDER_ENTRY)
                }
                else {
                    sendMessage()
                }
            }
        }
        else {
            sendMessage()
        }
    }

    private fun sendMessage() {
        val uuid = UUID.randomUUID().toString()
        val globalKey = UUID.randomUUID().toString()
        val partition = "${data.symbol}-${data.schoolSymbol}"

        val recipientsArray = JsonArray()
        recipients.forEach { teacher ->
            val loginId = teacher.loginId?.split(";", limit = 3) ?: return@forEach
            val key = loginId.getOrNull(0) ?: teacher.loginId
            val group = loginId.getOrNull(1)
            val name = loginId.getOrNull(2)
            if (key?.toIntOrNull() != null) {
                // raise error for old-format (non-UUID) login IDs
                data.error(TAG, ERROR_MESSAGE_NOT_SENT)
                return
            }
            recipientsArray += JsonObject(
                "Id" to "${data.messageBoxKey}-${key}",
                "Partition" to partition,
                "Owner" to data.messageBoxKey,
                "GlobalKey" to key,
                "Name" to name,
                "Group" to group,
                "Initials" to "",
                "HasRead" to 0,
            )
        }

        val sender = JsonObject(
            "Id" to "0",
            "Partition" to partition,
            "Owner" to data.messageBoxKey,
            "GlobalKey" to data.messageBoxKey,
            "Name" to data.messageBoxName,
            "Group" to "",
            "Initials" to "",
            "HasRead" to 0,
        )

        apiPost(
            TAG,
            VULCAN_HEBE_ENDPOINT_MESSAGEBOX_SEND,
            payload = JsonObject(
                "Id" to uuid,
                "GlobalKey" to globalKey,
                "Partition" to partition,
                "ThreadKey" to globalKey, // TODO correct threadKey for reply messages
                "Subject" to subject,
                "Content" to text,
                "Status" to 1,
                "Owner" to data.messageBoxKey,
                "DateSent" to buildDateTime(),
                "DateRead" to null,
                "Sender" to sender,
                "Receiver" to recipientsArray,
                "Attachments" to JsonArray(),
            )
        ) { _: JsonObject, _ ->
            // TODO handle errors

            VulcanHebeMessages(data, null) {
                val message = data.messageList.firstOrNull { it.isSent && it.subject == subject }
                // val metadata = data.metadataList.firstOrNull { it.thingType == Metadata.TYPE_MESSAGE && it.thingId == messageId }
                val event = MessageSentEvent(data.profileId, message, message?.addedDate)

                EventBus.getDefault().postSticky(event)
                onSuccess()
            }.getMessages(Message.TYPE_SENT)
        }
    }
}

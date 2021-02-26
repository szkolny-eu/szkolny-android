/*
 * Copyright (c) Kuba Szczodrzyński 2021-2-22.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe

import com.google.gson.JsonObject
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.VULCAN_HEBE_ENDPOINT_MESSAGES_SEND
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanHebe
import pl.szczodrzynski.edziennik.data.api.events.MessageSentEvent
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.Teacher

class VulcanHebeSendMessage(
    override val data: DataVulcan,
    val recipients: List<Teacher>,
    val subject: String,
    val text: String,
    val onSuccess: () -> Unit
) : VulcanHebe(data, null) {
    companion object {
        const val TAG = "VulcanHebeSendMessage"
    }

    init {
        val recipientsArray = JsonArray()
        recipients.forEach { teacher ->
            recipientsArray += JsonObject(
                "Address" to teacher.fullNameLastFirst,
                "LoginId" to (teacher.loginId?.toIntOrNull() ?: return@forEach),
                "Initials" to teacher.initialsLastFirst,
                "AddressHash" to teacher.fullNameLastFirst.sha1Hex()
            )
        }

        val senderName = (profile?.accountName ?: profile?.studentNameLong)
            ?.swapFirstLastName() ?: ""
        val sender = JsonObject(
            "Address" to senderName,
            "LoginId" to data.studentLoginId.toString(),
            "Initials" to senderName.getNameInitials(),
            "AddressHash" to senderName.sha1Hex()
        )

        apiPost(
            TAG,
            VULCAN_HEBE_ENDPOINT_MESSAGES_SEND,
            payload = JsonObject(
                "Status" to 1,
                "Sender" to sender,
                "DateSent" to null,
                "DateRead" to null,
                "Content" to text,
                "Receiver" to recipientsArray,
                "Id" to 0,
                "Subject" to subject,
                "Attachments" to null,
                "Self" to null
            )
        ) { json: JsonObject, _ ->
            val messageId = json.getLong("Id")

            if (messageId == null) {
                // TODO error
                return@apiPost
            }

            VulcanHebeMessages(data, null) {
                val message = data.messageList.firstOrNull { it.type == Message.TYPE_SENT && it.subject == subject }
                val metadata = data.metadataList.firstOrNull { it.thingType == Metadata.TYPE_MESSAGE && it.thingId == messageId }
                val event = MessageSentEvent(data.profileId, message, message?.addedDate)

                EventBus.getDefault().postSticky(event)
                onSuccess()
            }.getMessages(Message.TYPE_SENT)
        }
    }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-2.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.messages

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.base64Encode
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusMessages
import pl.szczodrzynski.edziennik.data.api.events.MessageSentEvent
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.getJsonObject
import pl.szczodrzynski.edziennik.getLong
import pl.szczodrzynski.edziennik.getString

class LibrusMessagesSendMessage(
        override val data: DataLibrus,
        val recipients: List<Teacher>,
        val subject: String,
        val text: String,
        val onSuccess: () -> Unit
) : LibrusMessages(data) {
    companion object {
        const val TAG = "LibrusMessages"
    }

    init {
        val params = mapOf<String, Any>(
                "topic" to subject.base64Encode(),
                "message" to text.base64Encode(),
                "receivers" to recipients
                        .filter { it.loginId != null }
                        .joinToString(",") { it.loginId ?: "" },
                "actions" to "<Actions/>".base64Encode()
        )

        messagesGetJson(TAG, "SendMessage", parameters = params) { json ->

            val response = json.getJsonObject("response").getJsonObject("SendMessage")
            val id = response.getLong("data")

            if (response.getString("status") != "ok" || id == null) {
                val message = response.getString("message")
                // TODO error
                return@messagesGetJson
            }

            LibrusMessagesGetList(data, type = Message.TYPE_SENT) {
                val message = data.messageIgnoreList.firstOrNull { it.type == Message.TYPE_SENT && it.id == id }
                val metadata = data.metadataList.firstOrNull { it.thingType == Metadata.TYPE_MESSAGE && it.thingId == message?.id }
                val event = MessageSentEvent(data.profileId, message, metadata?.addedDate)

                EventBus.getDefault().postSticky(event)
                onSuccess()
            }
        }
    }
}

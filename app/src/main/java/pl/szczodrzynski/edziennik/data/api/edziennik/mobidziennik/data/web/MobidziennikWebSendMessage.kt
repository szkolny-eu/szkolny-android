/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-26.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.web

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.data.api.POST
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.api.events.MessageSentEvent
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.Teacher

class MobidziennikWebSendMessage(override val data: DataMobidziennik,
                                 val recipients: List<Teacher>,
                                 val subject: String,
                                 val text: String,
                                 val onSuccess: () -> Unit
) : MobidziennikWeb(data, null) {
    companion object {
        private const val TAG = "MobidziennikWebSendMessage"
    }

    init {
        val params = mutableListOf<Pair<String, Any>>(
                "nazwa" to subject,
                "tresc" to text
        )
        for (teacher in recipients) {
            teacher.loginId?.let {
                params += "odbiorcy[]" to it
            }
        }

        webGet(TAG, endpoint = "/dziennik/dodajwiadomosc", method = POST, parameters = params) { text ->

            if (!text.contains(">Wiadomość została wysłana.<")) {
                // TODO error
                return@webGet
            }

            // TODO create MobidziennikWebMessagesSent and replace this
            MobidziennikWebMessagesAll(data, null) {
                val message = data.messageIgnoreList.firstOrNull { it.type == Message.TYPE_SENT && it.subject == subject }
                val metadata = data.metadataList.firstOrNull { it.thingType == Metadata.TYPE_MESSAGE && it.thingId == message?.id }
                val event = MessageSentEvent(data.profileId, message, metadata?.addedDate)

                EventBus.getDefault().postSticky(event)
                onSuccess()
            }
        }
    }
}

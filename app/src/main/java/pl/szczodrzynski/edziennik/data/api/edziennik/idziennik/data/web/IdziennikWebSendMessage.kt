/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-30.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.web

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA
import pl.szczodrzynski.edziennik.data.api.IDZIENNIK_WEB_SEND_MESSAGE
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.IdziennikWeb
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.api.IdziennikApiMessagesSent
import pl.szczodrzynski.edziennik.data.api.events.MessageSentEvent
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import java.util.*

class IdziennikWebSendMessage(override val data: DataIdziennik,
                              val recipients: List<Teacher>,
                              val subject: String,
                              val text: String,
                              val onSuccess: () -> Unit
) : IdziennikWeb(data, null) {
    companion object {
        private const val TAG = "IdziennikWebSendMessage"
    }

    init {
        val recipientsArray = JsonArray()
        for (teacher in recipients) {
            teacher.loginId?.let {
                recipientsArray += it
            }
        }

        webApiGet(TAG, IDZIENNIK_WEB_SEND_MESSAGE, mapOf(
                "Wiadomosc" to JsonObject(
                        "Tytul" to subject,
                        "Tresc" to text,
                        "Confirmation" to false,
                        "GuidMessage" to UUID.randomUUID().toString().toUpperCase(Locale.ROOT),
                        "Odbiorcy" to recipientsArray
                )
        )) { result ->
            val json = result.getJsonObject("d") ?: run {
                data.error(ApiError(TAG, ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA)
                        .withApiResponse(result))
                return@webApiGet
            }

            if (json.getBoolean("CzyJestBlad") != false) {
                // TODO error
                return@webApiGet
            }

            IdziennikApiMessagesSent(data, null) {
                val message = data.messageIgnoreList.firstOrNull { it.type == Message.TYPE_SENT && it.subject == subject }
                val metadata = data.metadataList.firstOrNull { it.thingType == Metadata.TYPE_MESSAGE && it.thingId == message?.id }
                val event = MessageSentEvent(data.profileId, message, metadata?.addedDate)

                EventBus.getDefault().postSticky(event)
                onSuccess()
            }
        }
    }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-29.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.api

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.VULCAN_API_ENDPOINT_MESSAGES_ADD
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanApi
import pl.szczodrzynski.edziennik.data.api.events.MessageSentEvent
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.Teacher

class VulcanApiSendMessage(
        override val data: DataVulcan,
        val recipients: List<Teacher>,
        val subject: String,
        val text: String,
        val onSuccess: () -> Unit
) : VulcanApi(data) {
    companion object {
        private const val TAG = "VulcanApiSendMessage"
    }

    init {
        val recipientsArray = JsonArray()
        for (teacher in recipients) {
            teacher.loginId?.let {
                recipientsArray += JsonObject(
                        "LoginId" to it,
                        "Nazwa" to "${teacher.fullNameLastFirst} - pracownik"
                )
            }
        }
        val params = mapOf(
                "NadawcaWiadomosci" to (profile?.accountName ?: profile?.studentNameLong ?: ""),
                "Tytul" to subject,
                "Tresc" to text,
                "Adresaci" to recipientsArray,
                "LoginId" to data.studentLoginId,
                "IdUczen" to data.studentId
        )

        apiGet(TAG, VULCAN_API_ENDPOINT_MESSAGES_ADD, parameters = params) { json, _ ->
            val messageId = json.getJsonObject("Data").getLong("WiadomoscId")

            if (messageId == null) {
                // TODO error
                return@apiGet
            }

            VulcanApiMessagesSent(data) {
                val message = data.messageIgnoreList.firstOrNull { it.type == Message.TYPE_SENT && it.subject == subject }
                val metadata = data.metadataList.firstOrNull { it.thingType == Metadata.TYPE_MESSAGE && it.thingId == messageId }
                val event = MessageSentEvent(data.profileId, message, metadata?.addedDate)

                EventBus.getDefault().postSticky(event)
                onSuccess()
            }
        }
    }
}

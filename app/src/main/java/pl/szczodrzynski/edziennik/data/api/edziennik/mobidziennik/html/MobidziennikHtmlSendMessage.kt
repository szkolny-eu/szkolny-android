package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.html

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.data.api.ERROR_MESSAGE_NOT_SENT
import pl.szczodrzynski.edziennik.data.api.POST
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.api.events.MessageSentEvent
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.Teacher

class MobidziennikHtmlSendMessage(
    override val data: DataMobidziennik,
    val recipients: Set<Teacher>,
    val subject: String,
    val text: String,
    val onSuccess: () -> Unit,
) : MobidziennikWeb(data, null) {
    companion object {
        private const val TAG = "MobidziennikHtmlSendMessage"
    }

    init {
        val params = mutableListOf<Pair<String, Any>>(
            "nazwa" to subject,
            "tresc" to text,
        )
        for (teacher in recipients) {
            teacher.loginId?.let {
                params += "odbiorcy[]" to it
            }
        }

        webGet(TAG, "/dziennik/dodajwiadomosc", method = POST, parameters = params) { response ->
            if (!response.contains(">Wiadomość została wysłana.<")) {
                data.error(ApiError(TAG, ERROR_MESSAGE_NOT_SENT).withApiResponse(response))
                return@webGet
            }

            MobidziennikHtmlMessagesSent(data, null) {
                val message = data.messageList.firstOrNull { it.isSent && it.subject == subject }
                EventBus.getDefault().postSticky(MessageSentEvent(data.profileId, message, message?.addedDate))
                onSuccess()
            }
        }
    }
}

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.synergia

import pl.szczodrzynski.edziennik.data.api.LIBRUS_SYNERGIA_MESSAGES_ATTACHMENT_URL
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusSynergia
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.messages.LibrusSandboxDownloadAttachment
import pl.szczodrzynski.edziennik.data.db.entity.Message

class LibrusSynergiaGetAttachment(override val data: DataLibrus,
                                  val message: Message,
                                  val attachmentId: Long,
                                  val attachmentName: String,
                                  val onSuccess: () -> Unit
) : LibrusSynergia(data, null) {
    companion object {
        const val TAG = "LibrusSynergiaGetAttachment"
    }

    init {
        redirectUrlGet(TAG, "$LIBRUS_SYNERGIA_MESSAGES_ATTACHMENT_URL/${message.id}/$attachmentId") { url ->
            LibrusSandboxDownloadAttachment(data, url, message, attachmentId, attachmentName, onSuccess)
        }
    }
}

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.synergia

import pl.szczodrzynski.edziennik.data.api.LIBRUS_SYNERGIA_HOMEWORK_ATTACHMENT_URL
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusSynergia
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.messages.LibrusSandboxDownloadAttachment
import pl.szczodrzynski.edziennik.data.db.full.EventFull

class LibrusSynergiaHomeworkGetAttachment(
        override val data: DataLibrus,
        val event: EventFull,
        val attachmentId: Long,
        val attachmentName: String,
        val onSuccess: () -> Unit
) : LibrusSynergia(data, null) {
    companion object {
        const val TAG = "LibrusSynergiaHomeworkGetAttachment"
    }

    init {
        redirectUrlGet(TAG, "$LIBRUS_SYNERGIA_HOMEWORK_ATTACHMENT_URL/$attachmentId") { url ->
            LibrusSandboxDownloadAttachment(data, url, event, attachmentId, attachmentName, onSuccess)
        }
    }
}

/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-28
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.web

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.data.api.IDZIENNIK_WEB_GET_ATTACHMENT
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.IdziennikWeb
import pl.szczodrzynski.edziennik.data.api.events.AttachmentGetEvent
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.get
import pl.szczodrzynski.edziennik.utils.Utils
import java.io.File

class IdziennikWebGetAttachment(
        override val data: DataIdziennik, val message: Message, val attachmentId: Long,
        val attachmentName: String, val onSuccess: () -> Unit
) : IdziennikWeb(data) {
    companion object {
        const val TAG = "IdziennikWebGetAttachment"
    }

    init {
        val messageId = "\\[META:([A-z0-9]+);([0-9-]+)]".toRegex().find(message.body ?: "")?.get(2) ?: -1
        val targetFile = File(Utils.getStorageDir(), attachmentName)

        webGetFile(TAG, IDZIENNIK_WEB_GET_ATTACHMENT, targetFile, mapOf(
                "id" to messageId,
                "fileName" to attachmentName
        ), { file ->
            val event = AttachmentGetEvent(
                    profileId,
                    message.id,
                    attachmentId,
                    AttachmentGetEvent.TYPE_FINISHED,
                    file.absolutePath
            )

            val attachmentDataFile = File(Utils.getStorageDir(), ".${profileId}_${event.messageId}_${event.attachmentId}")
            Utils.writeStringToFile(attachmentDataFile, event.fileName)

            EventBus.getDefault().post(event)

            onSuccess()

        }) { written, _ ->
            val event = AttachmentGetEvent(
                    profileId,
                    message.id,
                    attachmentId,
                    AttachmentGetEvent.TYPE_PROGRESS,
                    bytesWritten = written
            )

            EventBus.getDefault().post(event)
        }
    }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-28.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.web

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.api.events.AttachmentGetEvent
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.edziennik.utils.models.Date
import java.io.File

class MobidziennikWebGetAttachment(override val data: DataMobidziennik,
                                   val owner: Any,
                                   val attachmentId: Long,
                                   val attachmentName: String,
                                   val onSuccess: () -> Unit
) : MobidziennikWeb(data, null) {
    companion object {
        private const val TAG = "MobidziennikWebGetAttachment"
    }

    init {
        val targetFile = File(Utils.getStorageDir(), attachmentName)

        val typeUrl = when (owner) {
            is Message -> if (owner.isSent)
                "dziennik/wiadwyslana/?id="
            else
                "dziennik/wiadodebrana/?id="

            is Event -> if (owner.date >= Date.getToday())
                "dziennik/wyslijzadanie/?id_zadania="
            else
                "dziennik/wyslijzadanie/?id_zadania="

            else -> ""
        }

        val ownerId = when (owner) {
            is Message -> owner.id
            is Event -> owner.id
            else -> -1
        }

        webGetFile(TAG, "/$typeUrl${ownerId}&uczen=${data.studentId}&zalacznik=$attachmentId", targetFile, { file ->

            val event = AttachmentGetEvent(
                    profileId,
                    owner,
                    attachmentId,
                    AttachmentGetEvent.TYPE_FINISHED,
                    file.absolutePath
            )

            val attachmentDataFile = File(Utils.getStorageDir(), ".${profileId}_${event.ownerId}_${event.attachmentId}")
            Utils.writeStringToFile(attachmentDataFile, event.fileName)

            EventBus.getDefault().postSticky(event)

            onSuccess()

        }) { written, _ ->
            // TODO make use of bytesTotal
            val event = AttachmentGetEvent(
                    profileId,
                    owner,
                    attachmentId,
                    AttachmentGetEvent.TYPE_PROGRESS,
                    bytesWritten = written
            )

            EventBus.getDefault().postSticky(event)
        }
    }
}

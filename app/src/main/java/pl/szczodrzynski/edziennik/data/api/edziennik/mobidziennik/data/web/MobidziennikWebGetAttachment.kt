/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-28.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.web

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.api.events.AttachmentGetEvent
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.utils.Utils
import java.io.File

class MobidziennikWebGetAttachment(
        override val data: DataMobidziennik, val message: Message, val attachmentId: Long,
        val attachmentName: String, val onSuccess: () -> Unit) : MobidziennikWeb(data) {
    companion object {
        private const val TAG = "MobidziennikWebGetAttachment"
    }

    init {
        val targetFile = File(Utils.getStorageDir(), attachmentName)

        val typeUrl = if (message.type == Message.TYPE_SENT)
            "wiadwyslana"
        else
            "wiadodebrana"

        webGetFile(TAG, "/dziennik/$typeUrl/?id=${message.id}&zalacznik=$attachmentId", targetFile, { file ->

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
            // TODO make use of bytesTotal
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

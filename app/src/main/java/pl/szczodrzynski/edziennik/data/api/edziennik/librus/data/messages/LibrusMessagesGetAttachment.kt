/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-24
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.messages

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusMessages
import pl.szczodrzynski.edziennik.data.db.entity.Message
import kotlin.coroutines.CoroutineContext

class LibrusMessagesGetAttachment(override val data: DataLibrus,
                                  val message: Message,
                                  val attachmentId: Long,
                                  val attachmentName: String,
                                  val onSuccess: () -> Unit
) : LibrusMessages(data, null), CoroutineScope {
    companion object {
        const val TAG = "LibrusMessagesGetAttachment"
    }

    private var job = Job()

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    init {
        messagesGet(TAG, "GetFileDownloadLink", parameters = mapOf(
                "fileId" to attachmentId,
                "msgId" to message.id,
                "archive" to 0
        )) { doc ->
            val downloadLink = doc.select("response GetFileDownloadLink downloadLink").text()

            LibrusSandboxDownloadAttachment(data, downloadLink, message, attachmentId, attachmentName, onSuccess)
        }
    }
}

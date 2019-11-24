/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-24
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.messages

import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.api.v2.ERROR_FILE_DOWNLOAD
import pl.szczodrzynski.edziennik.api.v2.EXCEPTION_LIBRUS_MESSAGES_REQUEST
import pl.szczodrzynski.edziennik.api.v2.Regexes
import pl.szczodrzynski.edziennik.api.v2.events.AttachmentGetEvent
import pl.szczodrzynski.edziennik.api.v2.events.AttachmentGetEvent.Companion.TYPE_FINISHED
import pl.szczodrzynski.edziennik.api.v2.events.AttachmentGetEvent.Companion.TYPE_PROGRESS
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusMessages
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.get
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.utils.Utils
import java.io.File
import kotlin.coroutines.CoroutineContext


class LibrusMessagesGetAttachment(override val data: DataLibrus, val messageId: Long, val attachmentId: Long,
                                  val attachmentName: String, val onSuccess: () -> Unit) : LibrusMessages(data), CoroutineScope {
    companion object {
        const val TAG = "LibrusMessagesGetAttachment"
    }

    private var job = Job()

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Default

    private var getAttachmentCheckKeyTries = 0

    init {
        messagesGet(TAG, "GetFileDownloadLink", parameters = mapOf(
                "fileId" to attachmentId,
                "msgId" to messageId,
                "archive" to 0
        )) { doc ->
            val downloadLink = doc.select("response GetFileDownloadLink downloadLink").text()
            val keyMatcher = Regexes.LIBRUS_ATTACHMENT_KEY.find(downloadLink)

            if (keyMatcher != null) {
                getAttachmentCheckKeyTries = 0

                val attachmentKey = keyMatcher[1]
                getAttachmentCheckKey(attachmentKey) {
                    downloadAttachment(attachmentKey)
                }
            } else {
                data.error(ApiError(TAG, ERROR_FILE_DOWNLOAD)
                        .withApiResponse(doc.toString()))
            }

            onSuccess()
        }
    }

    private fun getAttachmentCheckKey(attachmentKey: String, callback: () -> Unit) {
        sandboxGet(TAG, "CSCheckKey",
                parameters = mapOf("singleUseKey" to attachmentKey)) { json ->

            when (json.getString("status")) {
                "not_downloaded_yet" -> {
                    if (getAttachmentCheckKeyTries++ > 5) {
                        data.error(ApiError(TAG, ERROR_FILE_DOWNLOAD)
                                .withApiResponse(json))
                        return@sandboxGet
                    }
                    launch {
                        delay(2000)
                        getAttachmentCheckKey(attachmentKey, callback)
                    }
                }

                "ready" -> {
                    launch { callback() }
                }

                else -> {
                    data.error(ApiError(TAG, EXCEPTION_LIBRUS_MESSAGES_REQUEST)
                            .withApiResponse(json))
                }
            }
        }
    }

    private fun downloadAttachment(attachmentKey: String) {
        val targetFile = File(Utils.getStorageDir(), attachmentName)

        sandboxGetFile(TAG, "CSDownload&singleUseKey=$attachmentKey",
                targetFile, { file ->

            val event = AttachmentGetEvent(
                    profileId,
                    messageId,
                    attachmentId,
                    TYPE_FINISHED,
                    file.absolutePath
            )

            val attachmentDataFile = File(Utils.getStorageDir(), ".${profileId}_${event.messageId}_${event.attachmentId}")
            Utils.writeStringToFile(attachmentDataFile, event.fileName)

            EventBus.getDefault().post(event)

        }) { written, _ ->
            val event = AttachmentGetEvent(
                    profileId,
                    messageId,
                    attachmentId,
                    TYPE_PROGRESS,
                    bytesWritten = written
            )

            EventBus.getDefault().post(event)
        }
    }
}

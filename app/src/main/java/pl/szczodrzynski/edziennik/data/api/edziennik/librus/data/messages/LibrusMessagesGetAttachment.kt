/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-24
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.messages

import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusMessages
import pl.szczodrzynski.edziennik.data.api.events.AttachmentGetEvent
import pl.szczodrzynski.edziennik.data.api.events.AttachmentGetEvent.Companion.TYPE_FINISHED
import pl.szczodrzynski.edziennik.data.api.events.AttachmentGetEvent.Companion.TYPE_PROGRESS
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.get
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.utils.Utils
import java.io.File
import kotlin.coroutines.CoroutineContext

class LibrusMessagesGetAttachment(override val data: DataLibrus,
                                  val owner: Any,
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

    private var getAttachmentCheckKeyTries = 0

    init {
        val message = owner as Message

        messagesGet(TAG, "GetFileDownloadLink", parameters = mapOf(
                "fileId" to attachmentId,
                "msgId" to message.id,
                "archive" to 0
        )) { doc ->
            val downloadLink = doc.select("response GetFileDownloadLink downloadLink").text()
            val keyMatcher = Regexes.LIBRUS_ATTACHMENT_KEY.find(downloadLink)

            if (keyMatcher != null) {
                getAttachmentCheckKeyTries = 0

                val attachmentKey = keyMatcher[1]
                getAttachmentCheckKey(attachmentKey) {
                    downloadAttachment("${LIBRUS_SANDBOX_URL}CSDownload&singleUseKey=$attachmentKey", method = POST)
                }
            } else {
                downloadAttachment("$downloadLink/get", method = GET)
            }
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

    private fun downloadAttachment(url: String, method: Int = GET) {
        val targetFile = File(Utils.getStorageDir(), attachmentName)

        sandboxGetFile(TAG, url, targetFile, { file ->

            val event = AttachmentGetEvent(
                    profileId,
                    owner,
                    attachmentId,
                    TYPE_FINISHED,
                    file.absolutePath
            )

            val attachmentDataFile = File(Utils.getStorageDir(), ".${profileId}_${event.ownerId}_${event.attachmentId}")
            Utils.writeStringToFile(attachmentDataFile, event.fileName)

            EventBus.getDefault().postSticky(event)

            onSuccess()

        }) { written, _ ->
            val event = AttachmentGetEvent(
                    profileId,
                    owner,
                    attachmentId,
                    TYPE_PROGRESS,
                    bytesWritten = written
            )

            EventBus.getDefault().postSticky(event)
        }
    }
}

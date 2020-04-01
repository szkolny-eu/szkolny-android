/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-1.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.web

import com.google.gson.JsonObject
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.data.api.IDZIENNIK_WEB_GET_HOMEWORK_ATTACHMENT
import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.IdziennikWeb
import pl.szczodrzynski.edziennik.data.api.events.AttachmentGetEvent
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.get
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.set
import pl.szczodrzynski.edziennik.utils.Utils
import java.io.File

class IdziennikWebGetHomeworkAttachment(override val data: DataIdziennik,
                                        val owner: Any,
                                        val attachmentId: Long,
                                        val attachmentName: String,
                                        val onSuccess: () -> Unit
) : IdziennikWeb(data, null) {
    companion object {
        const val TAG = "IdziennikWebGetHomeworkAttachment"
    }

    init {
        val homework = owner as Event

        /*val request = Request.Builder()
                .url("")
                .build()
        data.app.http.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                data.error(ApiError(TAG, ERROR_REQUEST_FAILURE)
                        .withThrowable(e))
            }

            override fun onResponse(call: Call, response: Response) {
                val filename = response.header("content-disposition")?.substringAfter("\"")?.substringBeforeLast("\"")

                val file: File = File(Utils.getStorageDir(), filename)
                val sink = file.sink().buffer()
                response.body()?.source()?.let {
                    sink.writeAll(it)
                }
                sink.close()
            }
        })*/

        webGet(TAG, IDZIENNIK_WEB_GET_HOMEWORK_ATTACHMENT) { text ->
            val hiddenFields = JsonObject()
            Regexes.IDZIENNIK_LOGIN_HIDDEN_FIELDS.findAll(text).forEach {
                hiddenFields[it[1]] = it[2]
            }

            webGetFile(TAG, IDZIENNIK_WEB_GET_HOMEWORK_ATTACHMENT, Utils.getStorageDir(), mapOf(
                    "__VIEWSTATE" to hiddenFields.getString("__VIEWSTATE", ""),
                    "__VIEWSTATEGENERATOR" to hiddenFields.getString("__VIEWSTATEGENERATOR", ""),
                    "__EVENTVALIDATION" to hiddenFields.getString("__EVENTVALIDATION", ""),
                    "__EVENTTARGET" to "ctl00\$cphContent\$bt_pobraniePliku",
                    "ctl00\$dxComboUczniowie" to data.registerId,
                    "ctl00\$cphContent\$idPracyDomowej" to attachmentId
            ), { file ->
                val event = AttachmentGetEvent(
                        profileId,
                        owner,
                        attachmentId,
                        AttachmentGetEvent.TYPE_FINISHED,
                        file.absolutePath
                )

                val attachmentDataFile = File(Utils.getStorageDir(), ".${profileId}_${event.ownerId}_${event.attachmentId}")
                Utils.writeStringToFile(attachmentDataFile, event.fileName)

                homework.attachmentNames = mutableListOf(file.name)
                data.eventList.add(homework)
                data.eventListReplace = true

                EventBus.getDefault().postSticky(event)
                onSuccess()

            }) { written, _ ->
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
}

/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-31.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.web

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.api.events.EventGetEvent
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.ext.get
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class MobidziennikWebGetHomework(override val data: DataMobidziennik,
                                 val event: EventFull,
                                 val onSuccess: () -> Unit
) : MobidziennikWeb(data, null) {
    companion object {
        private const val TAG = "MobidziennikWebGetHomework"
    }

    init {
        webGet(TAG, "/dziennik/wyslijzadanie/?id_zadania=${event.id}&uczen=${data.studentId}") { text ->
            MobidziennikLuckyNumberExtractor(data, text)

            event.clearAttachments()
            Regexes.MOBIDZIENNIK_WEB_ATTACHMENT.findAll(text).forEach { match ->
                if (match[1].isNotEmpty())
                    return@forEach
                val attachmentId = match[2].toLong()
                val attachmentName = match[3]
                event.addAttachment(attachmentId, attachmentName)
            }

            Regexes.MOBIDZIENNIK_WEB_HOMEWORK_ADDED_DATE.find(text)?.let {
                // (Kowalski Jan), (wtorek), (2) (stycznia) (2019), godzina (12:34:56)
                val month = when (it[4]) {
                    "stycznia" -> 1
                    "lutego" -> 2
                    "marca" -> 3
                    "kwietnia" -> 4
                    "maja" -> 5
                    "czerwca" -> 6
                    "lipca" -> 7
                    "sierpnia" -> 8
                    "września" -> 9
                    "października" -> 10
                    "listopada" -> 11
                    "grudnia" -> 12
                    else -> 1
                }
                val addedDate = Date(
                    it[5].toInt(),
                    month,
                    it[3].toInt()
                )
                val time = Time.fromH_m_s(it[6])
                event.addedDate = addedDate.combineWith(time)
            }

            event.homeworkBody = ""
            event.isDownloaded = true

            data.eventList.add(event)
            data.eventListReplace = true

            EventBus.getDefault().postSticky(EventGetEvent(event))
            onSuccess()
        }
    }
}

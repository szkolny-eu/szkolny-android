package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.html

import org.greenrobot.eventbus.EventBus
import org.jsoup.nodes.Entities
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.api.events.EventGetEvent
import pl.szczodrzynski.edziennik.data.db.full.EventFull

class MobidziennikHtmlGetHomework(
    override val data: DataMobidziennik,
    val event: EventFull,
    val onSuccess: () -> Unit,
) : MobidziennikWeb(data, null) {
    companion object {
        private const val TAG = "MobidziennikHtmlGetHomework"
    }

    init {
        webGet(TAG, "/dziennik/wyslijzadanie/?id_zadania=${event.id}&uczen=${data.studentId}") { text ->
            val details = HtmlParsers.parseHomeworkDetails(text, event.id.toString()) - "id"

            val textRows = details.filterKeys {
                val k = it.lowercase()
                "treść" in k || "tresc" in k || "opis" in k
            }
            val rows = textRows.ifEmpty { details }
            event.homeworkBody = if (textRows.isNotEmpty() && textRows.size == 1)
                Entities.escape(rows.values.first()).replace("\n", "<br>")
            else
                rows.entries.joinToString("<br>") { (k, v) ->
                    "<b>${Entities.escape(k)}</b>: ${Entities.escape(v).replace("\n", "<br>")}"
                }
            event.isDownloaded = true
            event.clearAttachments()
            htmlAttachments(text, skipAnswers = true).forEach { event.addAttachment(it.id, it.name) }

            data.eventList.add(event)
            data.eventListReplace = true

            EventBus.getDefault().postSticky(EventGetEvent(event))
            onSuccess()
        }
    }
}

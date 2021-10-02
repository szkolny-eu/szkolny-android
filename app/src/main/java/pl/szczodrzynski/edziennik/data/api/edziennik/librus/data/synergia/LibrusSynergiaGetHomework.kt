package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.synergia

import android.text.Html
import org.greenrobot.eventbus.EventBus
import org.jsoup.Jsoup
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusSynergia
import pl.szczodrzynski.edziennik.data.api.events.EventGetEvent
import pl.szczodrzynski.edziennik.data.db.full.EventFull

class LibrusSynergiaGetHomework(override val data: DataLibrus,
                                val event: EventFull,
                                val onSuccess: () -> Unit
) : LibrusSynergia(data, null) {
    companion object {
        const val TAG = "LibrusSynergiaGetHomework"
    }

    init {
        synergiaGet(TAG, "moje_zadania/podglad/${event.id}") { text ->
            val doc = Jsoup.parse(text)

            val table = doc.select("table.decorated tbody > tr")

            event.topic = table[1].select("td")[1].text()
            event.homeworkBody = Html.fromHtml(table[5].select("td")[1].html()).toString()
            event.isDownloaded = true

            event.attachmentIds = mutableListOf()
            event.attachmentNames = mutableListOf()

            if (table.size > 6) {
                table[6].select("a").forEach { a ->
                    val attachmentId = a.attr("href").split('/')
                            .last().toLongOrNull() ?: return@forEach
                    val filename = a.text()
                    event.attachmentIds?.add(attachmentId)
                    event.attachmentNames?.add(filename)
                }
            }

            data.eventList.add(event)
            data.eventListReplace = true

            EventBus.getDefault().postSticky(EventGetEvent(event))
            onSuccess()
        }
    }
}

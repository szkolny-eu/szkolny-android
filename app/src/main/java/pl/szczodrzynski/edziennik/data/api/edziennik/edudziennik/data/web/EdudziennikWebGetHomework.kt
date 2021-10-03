package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.web

import android.text.Html
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.EdudziennikWeb
import pl.szczodrzynski.edziennik.data.api.events.EventGetEvent
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.get
import pl.szczodrzynski.edziennik.isNotNullNorEmpty

class EdudziennikWebGetHomework(
        override val data: DataEdudziennik,
        val event: EventFull,
        val onSuccess: () -> Unit
) : EdudziennikWeb(data, null) {
    companion object {
        const val TAG = "EdudziennikWebGetHomework"
    }

    init {
        if (event.attachmentNames.isNotNullNorEmpty()) {
            val id = event.attachmentNames!![0]

            webGet(TAG, "Homework/$id") { text ->
                val description = Regexes.EDUDZIENNIK_HOMEWORK_DESCRIPTION.find(text)?.get(1)?.trim()

                if (description != null) event.topic = Html.fromHtml(description).toString()

                event.homeworkBody = ""
                event.isDownloaded = true
                event.attachmentNames = null

                data.eventList += event
                data.eventListReplace = true

                EventBus.getDefault().postSticky(EventGetEvent(event))
                onSuccess()
            }
        } else {
            EventBus.getDefault().postSticky(EventGetEvent(event))
            onSuccess()
        }
    }
}

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
import pl.szczodrzynski.edziennik.get
import pl.szczodrzynski.edziennik.utils.models.Date

class MobidziennikWebGetHomework(override val data: DataMobidziennik,
                                 val event: EventFull,
                                 val onSuccess: () -> Unit
) : MobidziennikWeb(data, null) {
    companion object {
        private const val TAG = "MobidziennikWebHomework"
    }

    init {
        val endpoint = if (event.date >= Date.getToday())
            "zadaniadomowe"
        else
            "zadaniadomowearchiwalne"

        webGet(TAG, "/mobile/$endpoint") { text ->
            MobidziennikLuckyNumberExtractor(data, text)

            Regexes.MOBIDZIENNIK_HOMEWORK_ROW.findAll(text).forEach { homeworkMatch ->
                val tableRow = homeworkMatch[1].ifBlank { return@forEach }

                val id = Regexes.MOBIDZIENNIK_HOMEWORK_ID.find(tableRow)?.get(1)?.toLongOrNull() ?: return@forEach
                if (event.id != id)
                    return@forEach

                event.attachmentIds = mutableListOf()
                event.attachmentNames = mutableListOf()
                Regexes.MOBIDZIENNIK_HOMEWORK_ATTACHMENT.findAll(tableRow).forEach {
                    event.attachmentIds?.add(it[2].toLongOrNull() ?: return@forEach)
                    event.attachmentNames?.add(it[3])
                }

                event.homeworkBody = ""
            }

            data.eventList.add(event)
            data.eventListReplace = true

            EventBus.getDefault().postSticky(EventGetEvent(event))
            onSuccess()
        }
    }
}

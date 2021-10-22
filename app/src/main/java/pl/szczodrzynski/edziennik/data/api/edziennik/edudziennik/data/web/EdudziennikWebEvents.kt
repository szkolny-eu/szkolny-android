/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-1
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.web

import org.jsoup.Jsoup
import pl.szczodrzynski.edziennik.data.api.Regexes.EDUDZIENNIK_EVENT_ID
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.ENDPOINT_EDUDZIENNIK_WEB_EVENTS
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.EdudziennikWeb
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.ext.crc32
import pl.szczodrzynski.edziennik.ext.get
import pl.szczodrzynski.edziennik.utils.models.Date

class EdudziennikWebEvents(override val data: DataEdudziennik,
                           override val lastSync: Long?,
                           val onSuccess: (endpointId: Int) -> Unit
) : EdudziennikWeb(data, lastSync) {
    companion object {
        const val TAG = "EdudziennikWebEvents"
    }

    init { data.profile?.also { profile ->
        webGet(TAG, data.studentAndClassesEndpoint + "KlassEvent", xhr = true) { text ->
            val doc = Jsoup.parseBodyFragment("<table>" + text.trim() + "</table>")

            doc.getElementsByTag("tr").forEach { eventElement ->
                val date = Date.fromY_m_d(eventElement.child(1).text())

                val titleElement = eventElement.child(2).child(0)
                val title = titleElement.text().trim()

                val id = EDUDZIENNIK_EVENT_ID.find(titleElement.attr("href"))?.get(1)?.crc32()
                        ?: return@forEach

                val eventObject = Event(
                        profileId = profileId,
                        id = id,
                        date = date,
                        time = null,
                        topic = title,
                        color = null,
                        type = Event.TYPE_CLASS_EVENT,
                        teacherId = -1,
                        subjectId = -1,
                        teamId = data.teamClass?.id ?: -1
                )

                data.eventList.add(eventObject)
                data.metadataList.add(Metadata(
                        profileId,
                        Metadata.TYPE_EVENT,
                        id,
                        profile.empty,
                        profile.empty
                ))
            }

            data.toRemove.add(DataRemoveModel.Events.futureWithType(Event.TYPE_CLASS_EVENT))

            data.setSyncNext(ENDPOINT_EDUDZIENNIK_WEB_EVENTS, SYNC_ALWAYS)
            onSuccess(ENDPOINT_EDUDZIENNIK_WEB_EVENTS)
        }
    } ?: onSuccess(ENDPOINT_EDUDZIENNIK_WEB_EVENTS) }
}

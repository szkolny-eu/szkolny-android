/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-20
 */

package pl.szczodrzynski.edziennik.api.v2.vulcan.data.api

import pl.szczodrzynski.edziennik.api.v2.VULCAN_API_ENDPOINT_EVENTS
import pl.szczodrzynski.edziennik.api.v2.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.api.v2.vulcan.ENDPOINT_VULCAN_API_EVENTS
import pl.szczodrzynski.edziennik.api.v2.vulcan.data.VulcanApi
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.events.Event
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.getBoolean
import pl.szczodrzynski.edziennik.getJsonArray
import pl.szczodrzynski.edziennik.getLong
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.utils.models.Date

class VulcanApiEvents(override val data: DataVulcan, val onSuccess: () -> Unit) : VulcanApi(data) {
    companion object {
        const val TAG = "VulcanApi"
    }

    init {
        apiGet(TAG, VULCAN_API_ENDPOINT_EVENTS) { json, _ ->
            val events = json.getJsonArray("Data")

            events?.forEach { eventEl ->
                val event = eventEl.asJsonObject

                val id = event?.getLong("Id") ?: return@forEach
                val eventDate = Date.fromY_m_d(event.getString("DataTekst") ?: return@forEach)
                val subjectId = event.getLong("IdPrzedmiot") ?: -1
                val teacherId = event.getLong("IdPracownik") ?: -1
                val startTime = data.lessonList.singleOrNull {
                    it.weekDay == eventDate.weekDay && it.subjectId == subjectId
                }?.startTime
                val topic = event.getString("Opis") ?: ""
                val type = when (event.getBoolean("Rodzaj")) {
                    true -> Event.TYPE_EXAM
                    else -> Event.TYPE_SHORT_QUIZ
                }
                val teamId = event.getLong("IdOddzial") ?: data.teamClass?.id ?: return@forEach

                val eventObject = Event(
                        profileId,
                        id,
                        eventDate,
                        startTime,
                        topic,
                        -1,
                        type,
                        false,
                        teacherId,
                        subjectId,
                        teamId
                )

                data.eventList.add(eventObject)
                data.metadataList.add(Metadata(
                        profileId,
                        Metadata.TYPE_EVENT,
                        id,
                        profile?.empty ?: false,
                        profile?.empty ?: false,
                        System.currentTimeMillis()
                ))
            }

            data.setSyncNext(ENDPOINT_VULCAN_API_EVENTS, SYNC_ALWAYS)
            onSuccess()
        }
    }
}

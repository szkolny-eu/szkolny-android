/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-20
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.api

import pl.szczodrzynski.edziennik.data.api.VULCAN_API_ENDPOINT_EVENTS
import pl.szczodrzynski.edziennik.data.api.VULCAN_API_ENDPOINT_HOMEWORK
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_API_EVENTS
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_API_HOMEWORK
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanApi
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.getBoolean
import pl.szczodrzynski.edziennik.getJsonArray
import pl.szczodrzynski.edziennik.getLong
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.utils.models.Date

class VulcanApiEvents(override val data: DataVulcan,
                      override val lastSync: Long?,
                      private val isHomework: Boolean,
                      val onSuccess: (endpointId: Int) -> Unit
) : VulcanApi(data, lastSync) {
    companion object {
        const val TAG = "VulcanApiEvents"
    }

    init { data.profile?.also { profile ->

        val startDate: String = when (profile.empty) {
            true -> profile.getSemesterStart(profile.currentSemester).stringY_m_d
            else -> Date.getToday().stepForward(0, -1, 0).stringY_m_d
        }
        val endDate: String = profile.getSemesterEnd(profile.currentSemester).stringY_m_d

        val endpoint = when (isHomework) {
            true -> VULCAN_API_ENDPOINT_HOMEWORK
            else -> VULCAN_API_ENDPOINT_EVENTS
        }
        apiGet(TAG, endpoint, parameters = mapOf(
                "DataPoczatkowa" to startDate,
                "DataKoncowa" to endDate,
                "IdOddzial" to data.studentClassId,
                "IdUczen" to data.studentId,
                "IdOkresKlasyfikacyjny" to data.studentSemesterId
        )) { json, _ ->
            val events = json.getJsonArray("Data")

            events?.forEach { eventEl ->
                val event = eventEl.asJsonObject

                val id = event?.getLong("Id") ?: return@forEach
                val eventDate = Date.fromY_m_d(event.getString("DataTekst") ?: return@forEach)
                val subjectId = event.getLong("IdPrzedmiot") ?: -1
                val teacherId = event.getLong("IdPracownik") ?: -1
                val topic = event.getString("Opis") ?: ""

                val lessonList = data.db.timetableDao().getForDateNow(profileId, eventDate)
                val startTime = lessonList.firstOrNull { it.subjectId == subjectId }?.startTime

                val type = when (isHomework) {
                    true -> Event.TYPE_HOMEWORK
                    else -> when (event.getBoolean("Rodzaj")) {
                        false -> Event.TYPE_SHORT_QUIZ
                        else -> Event.TYPE_EXAM
                    }
                }
                val teamId = event.getLong("IdOddzial") ?: data.teamClass?.id ?: -1

                val eventObject = Event(
                        profileId = profileId,
                        id = id,
                        date = eventDate,
                        time = startTime,
                        topic = topic,
                        color = null,
                        type = type,
                        teacherId = teacherId,
                        subjectId = subjectId,
                        teamId = teamId
                )

                data.eventList.add(eventObject)
                data.metadataList.add(Metadata(
                        profileId,
                        if (isHomework) Metadata.TYPE_HOMEWORK else Metadata.TYPE_EVENT,
                        id,
                        profile.empty,
                        profile.empty,
                        System.currentTimeMillis()
                ))
            }

            when (isHomework) {
                true -> {
                    data.toRemove.add(DataRemoveModel.Events.futureWithType(Event.TYPE_HOMEWORK))
                    data.setSyncNext(ENDPOINT_VULCAN_API_HOMEWORK, SYNC_ALWAYS)
                }
                false -> {
                    data.toRemove.add(DataRemoveModel.Events.futureExceptType(Event.TYPE_HOMEWORK))
                    data.setSyncNext(ENDPOINT_VULCAN_API_EVENTS, SYNC_ALWAYS)
                }
            }
            onSuccess(if (isHomework) ENDPOINT_VULCAN_API_HOMEWORK else ENDPOINT_VULCAN_API_EVENTS)
        }
    } ?: onSuccess(if (isHomework) ENDPOINT_VULCAN_API_HOMEWORK else ENDPOINT_VULCAN_API_EVENTS) }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-24.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_PT_MEETINGS
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class LibrusApiPtMeetings(override val data: DataLibrus,
                          override val lastSync: Long?,
                          val onSuccess: (endpointId: Int) -> Unit
) : LibrusApi(data, lastSync) {
    companion object {
        const val TAG = "LibrusApiPtMeetings"
    }

    init {
        apiGet(TAG, "ParentTeacherConferences") { json ->
            val ptMeetings = json.getJsonArray("ParentTeacherConferences")?.asJsonObjectList()

            ptMeetings?.forEach { meeting ->
                val id = meeting.getLong("Id") ?: return@forEach
                val topic = meeting.getString("Topic") ?: ""
                val teacherId = meeting.getJsonObject("Teacher")?.getLong("Id") ?: -1
                val eventDate = meeting.getString("Date")?.let { Date.fromY_m_d(it) } ?: return@forEach
                val startTime = meeting.getString("Time")?.let {
                    if (it == "00:00:00")
                        null
                    else
                        Time.fromH_m_s(it)
                }

                val eventObject = Event(
                        profileId = profileId,
                        id = id,
                        date = eventDate,
                        time = startTime,
                        topic = topic,
                        color = null,
                        type = Event.TYPE_PT_MEETING,
                        teacherId = teacherId,
                        subjectId = -1,
                        teamId = data.teamClass?.id ?: -1
                )

                data.eventList.add(eventObject)
                data.metadataList.add(
                        Metadata(
                                profileId,
                                Metadata.TYPE_EVENT,
                                id,
                                profile?.empty ?: false,
                                profile?.empty ?: false,
                                System.currentTimeMillis()
                        ))
            }

            data.toRemove.add(DataRemoveModel.Events.futureWithType(Event.TYPE_PT_MEETING))

            data.setSyncNext(ENDPOINT_LIBRUS_API_PT_MEETINGS, 12*HOUR)
            onSuccess(ENDPOINT_LIBRUS_API_PT_MEETINGS)
        }
    }
}

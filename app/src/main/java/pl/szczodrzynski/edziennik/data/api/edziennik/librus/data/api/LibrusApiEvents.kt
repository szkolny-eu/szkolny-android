/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-4.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import androidx.core.util.isEmpty
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_EVENTS
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class LibrusApiEvents(override val data: DataLibrus,
                      val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiEvents"
    }

    init {
        if (data.eventTypes.isEmpty()) {
            data.db.eventTypeDao().getAllNow(profileId).toSparseArray(data.eventTypes) { it.id }
        }

        apiGet(TAG, "HomeWorks") { json ->
            val events = json.getJsonArray("HomeWorks")?.asJsonObjectList()

            events?.forEach { event ->
                val id = event.getLong("Id") ?: return@forEach
                val eventDate = Date.fromY_m_d(event.getString("Date"))
                val topic = event.getString("Content") ?: ""
                val type = event.getJsonObject("Category")?.getInt("Id") ?: -1
                val teacherId = event.getJsonObject("CreatedBy")?.getLong("Id") ?: -1
                val subjectId = event.getJsonObject("Subject")?.getLong("Id") ?: -1
                val teamId = event.getJsonObject("Class")?.getLong("Id") ?: -1

                val lessonNo = event.getInt("LessonNo")
                val lessonRange = data.lessonRanges.singleOrNull { it.lessonNumber == lessonNo }
                val startTime = lessonRange?.startTime ?: Time.fromH_m(event.getString("TimeFrom"))
                val addedDate = Date.fromIso(event.getString("AddDate"))

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
                data.metadataList.add(
                        Metadata(
                                profileId,
                                Metadata.TYPE_EVENT,
                                id,
                                profile?.empty ?: false,
                                profile?.empty ?: false,
                                addedDate
                        ))
            }

            data.toRemove.add(DataRemoveModel.Events.futureExceptTypes(listOf(
                    Event.TYPE_HOMEWORK,
                    Event.TYPE_PT_MEETING
            )))

            data.setSyncNext(ENDPOINT_LIBRUS_API_EVENTS, SYNC_ALWAYS)
            onSuccess()
        }
    }
}

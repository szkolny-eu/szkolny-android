/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-12.
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.ENDPOINT_LIBRUS_API_HOMEWORK
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.events.Event
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.utils.models.Date

class LibrusApiHomework(override val data: DataLibrus,
                        val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiHomework"
    }

    init {
        apiGet(TAG, "HomeWorkAssignments") { json ->
            val homeworkList = json.getJsonArray("HomeWorkAssignments").asJsonObjectList()

            homeworkList?.forEach { homework ->
                val id = homework.getLong("Id") ?: return@forEach
                val eventDate = Date.fromY_m_d(homework.getString("DueDate"))
                val topic = homework.getString("Topic") + "\n" + homework.getString("Text")
                val teacherId = homework.getJsonObject("Teacher")?.getLong("Id") ?: -1
                val addedDate = Date.fromY_m_d(homework.getString("Date"))

                val eventObject = Event(
                        profileId,
                        id,
                        eventDate,
                        null,
                        topic,
                        -1,
                        -1,
                        false,
                        teacherId,
                        -1,
                        -1
                )

                data.eventList.add(eventObject)
                data.metadataList.add(Metadata(
                        profileId,
                        Metadata.TYPE_HOMEWORK,
                        id,
                        profile?.empty ?: false,
                        profile?.empty ?: false,
                        addedDate.inMillis
                ))
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_HOMEWORK, SYNC_ALWAYS)
            onSuccess()
        }
    }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-12.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_HOMEWORK
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.enums.MetadataType
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.utils.models.Date

class LibrusApiHomework(override val data: DataLibrus,
                        override val lastSync: Long?,
                        val onSuccess: (endpointId: Int) -> Unit
) : LibrusApi(data, lastSync) {
    companion object {
        const val TAG = "LibrusApiHomework"
    }

    init {
        apiGet(TAG, "HomeWorkAssignments") { json ->
            val homeworkList = json.getJsonArray("HomeWorkAssignments")?.asJsonObjectList()

            homeworkList?.forEach { homework ->
                val id = homework.getLong("Id") ?: return@forEach
                val eventDate = Date.fromY_m_d(homework.getString("DueDate"))
                val topic = homework.getString("Topic") + "\n" + homework.getString("Text")
                val teacherId = homework.getJsonObject("Teacher")?.getLong("Id") ?: -1
                val addedDate = Date.fromY_m_d(homework.getString("Date"))

                val eventObject = Event(
                        profileId = profileId,
                        id = id,
                        date = eventDate,
                        time = null,
                        topic = topic,
                        color = null,
                        type = -1,
                        teacherId = teacherId,
                        subjectId = -1,
                        teamId = -1,
                        addedDate = addedDate.inMillis
                )

                data.eventList.add(eventObject)
                data.metadataList.add(Metadata(
                        profileId,
                        MetadataType.HOMEWORK,
                        id,
                        profile?.empty ?: false,
                        profile?.empty ?: false
                ))
            }

            data.toRemove.add(DataRemoveModel.Events.futureWithType(Event.TYPE_HOMEWORK))

            data.setSyncNext(ENDPOINT_LIBRUS_API_HOMEWORK, SYNC_ALWAYS)
            onSuccess(ENDPOINT_LIBRUS_API_HOMEWORK)
        }
    }
}

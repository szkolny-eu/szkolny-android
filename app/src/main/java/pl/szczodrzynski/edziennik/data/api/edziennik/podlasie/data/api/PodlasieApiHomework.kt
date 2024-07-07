/*
 * Copyright (c) Kacper Ziubryniewicz 2020-5-14
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.data.api

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.DataPodlasie
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.enums.MetadataType
import pl.szczodrzynski.edziennik.ext.crc32
import pl.szczodrzynski.edziennik.ext.get
import pl.szczodrzynski.edziennik.ext.getString
import pl.szczodrzynski.edziennik.utils.models.Date

class PodlasieApiHomework(val data: DataPodlasie, val rows: List<JsonObject>) {
    init {
        rows.reversed().forEach { homework ->
            val id = homework.getString("ExternalId")?.crc32() ?: return@forEach
            val topic = homework.getString("Title")?.replace("&#34;", "\"") ?: ""
            val description = homework.getString("Message")?.replace("&#34;", "\"") ?: ""
            val date = Date.getToday()
            val addedDate = System.currentTimeMillis()

            val eventObject = Event(
                    profileId = data.profileId,
                    id = id,
                    date = date,
                    time = null,
                    topic = topic,
                    color = null,
                    type = Event.TYPE_HOMEWORK,
                    teacherId = -1,
                    subjectId = -1,
                    teamId = data.teamClass?.id ?: -1,
                    addedDate = addedDate
            ).apply {
                homeworkBody = description
                isDownloaded = true
            }

            eventObject.attachmentIds = mutableListOf()
            eventObject.attachmentNames = mutableListOf()
            homework.getString("Attachments")?.split(',')?.onEach { url ->
                val filename = "&filename=(.*?)&".toRegex().find(url)?.get(1) ?: return@onEach
                val ext = "&ext=(.*?)&".toRegex().find(url)?.get(1) ?: return@onEach
                eventObject.attachmentIds?.add(url.crc32())
                eventObject.attachmentNames?.add("$filename.$ext:$url")
            }

            data.eventList.add(eventObject)
            data.metadataList.add(
                    Metadata(
                            data.profileId,
                            MetadataType.HOMEWORK,
                            id,
                            data.profile?.empty ?: false,
                            data.profile?.empty ?: false
                    ))
        }

        data.toRemove.add(DataRemoveModel.Events.futureWithType(Event.TYPE_HOMEWORK))
    }
}

/*
 * Copyright (c) Kacper Ziubryniewicz 2020-5-13
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.data.api

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.DataPodlasie
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.ext.getLong
import pl.szczodrzynski.edziennik.ext.getString
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import java.util.*

class PodlasieApiEvents(val data: DataPodlasie, val rows: List<JsonObject>) {
    init {
        rows.forEach { event ->
            val id = event.getLong("ExternalId") ?: return@forEach
            val date = event.getString("DateFrom")?.let { Date.fromY_m_d(it) } ?: return@forEach
            val time = event.getString("DateFrom")?.let { Time.fromY_m_d_H_m_s(it) }
                    ?: return@forEach

            val name = event.getString("Name")?.replace("&#34;", "\"") ?: ""
            val description = event.getString("Description")?.replace("&#34;", "\"") ?: ""

            val type = when (event.getString("Category")?.toLowerCase(Locale.getDefault())) {
                "klasówka" -> Event.TYPE_EXAM
                "praca domowa" -> Event.TYPE_HOMEWORK
                "wycieczka" -> Event.TYPE_EXCURSION
                else -> Event.TYPE_DEFAULT
            }

            val teacherFirstName = event.getString("PersonEnteringDataFirstName") ?: return@forEach
            val teacherLastName = event.getString("PersonEnteringDataLastName") ?: return@forEach
            val teacher = data.getTeacher(teacherFirstName, teacherLastName)

            val lessonList = data.db.timetableDao().getAllForDateNow(data.profileId, date)
            val lesson = lessonList.firstOrNull { it.startTime == time }

            val addedDate = event.getString("CreateDate")?.let { Date.fromIso(it) }
                    ?: System.currentTimeMillis()

            val eventObject = Event(
                    profileId = data.profileId,
                    id = id,
                    date = date,
                    time = time,
                    topic = name,
                    color = null,
                    type = type,
                    teacherId = teacher.id,
                    subjectId = lesson?.subjectId ?: -1,
                    teamId = data.teamClass?.id ?: -1,
                    addedDate = addedDate
            ).apply {
                homeworkBody = description
                isDownloaded = true
            }

            data.eventList.add(eventObject)
            data.metadataList.add(
                    Metadata(
                            data.profileId,
                            if (type == Event.TYPE_HOMEWORK) Metadata.TYPE_HOMEWORK else Metadata.TYPE_EVENT,
                            id,
                            data.profile?.empty ?: false,
                            data.profile?.empty ?: false
                    ))
        }

        data.toRemove.add(DataRemoveModel.Events.future())
    }
}

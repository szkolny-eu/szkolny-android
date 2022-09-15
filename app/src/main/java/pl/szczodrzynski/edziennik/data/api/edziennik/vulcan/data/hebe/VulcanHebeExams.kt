/*
 * Copyright (c) Kuba Szczodrzyński 2021-2-21.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.VULCAN_HEBE_ENDPOINT_EXAMS
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_HEBE_EXAMS
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanHebe
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.ext.getLong
import pl.szczodrzynski.edziennik.ext.getString

class VulcanHebeExams(
    override val data: DataVulcan,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit
) : VulcanHebe(data, lastSync) {
    companion object {
        const val TAG = "VulcanHebeExams"
    }

    init {
        apiGetList(
            TAG,
            VULCAN_HEBE_ENDPOINT_EXAMS,
            HebeFilterType.BY_PUPIL,
            lastSync = lastSync
        ) { list, _ ->
            list.forEach { exam ->
                val id = exam.getLong("Id") ?: return@forEach
                val eventDate = getDate(exam, "Deadline") ?: return@forEach
                val subjectId = getSubjectId(exam, "Subject") ?: -1
                val teacherId = getTeacherId(exam, "Creator") ?: -1
                val teamId = getTeamId(exam, "Distribution")
                val topic = exam.getString("Content")?.trim() ?: ""

                if (!isCurrentYear(eventDate)) return@forEach

                val lessonList = data.db.timetableDao().getAllForDateNow(profileId, eventDate)
                val startTime = lessonList.firstOrNull { it.subjectId == subjectId }?.startTime

                val type = when (exam.getString("Type")) {
                    "Praca klasowa",
                    "Sprawdzian" -> Event.TYPE_EXAM
                    "Kartkówka" -> Event.TYPE_SHORT_QUIZ
                    else -> Event.TYPE_DEFAULT
                }

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
                data.metadataList.add(
                    Metadata(
                        profileId,
                        Metadata.TYPE_EVENT,
                        id,
                        profile?.empty ?: true,
                        profile?.empty ?: true
                    )
                )
            }

            data.setSyncNext(ENDPOINT_VULCAN_HEBE_EXAMS, SYNC_ALWAYS)
            onSuccess(ENDPOINT_VULCAN_HEBE_EXAMS)
        }
    }
}

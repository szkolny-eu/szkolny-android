/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-24
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.web

import org.jsoup.Jsoup
import pl.szczodrzynski.edziennik.crc32
import pl.szczodrzynski.edziennik.data.api.Regexes.EDUDZIENNIK_EVENT_TYPE_ID
import pl.szczodrzynski.edziennik.data.api.Regexes.EDUDZIENNIK_EXAM_ID
import pl.szczodrzynski.edziennik.data.api.Regexes.EDUDZIENNIK_SUBJECT_ID
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.ENDPOINT_EDUDZIENNIK_WEB_EXAMS
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.EdudziennikWeb
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.get
import pl.szczodrzynski.edziennik.utils.models.Date

class EdudziennikWebExams(override val data: DataEdudziennik,
                          override val lastSync: Long?,
                          val onSuccess: (endpointId: Int) -> Unit
) : EdudziennikWeb(data, lastSync) {
    companion object {
        const val TAG = "EdudziennikWebExams"
    }

    init { profile?.also { profile ->
        webGet(TAG, data.studentAndClassEndpoint + "Evaluations", xhr = true) { text ->
            val doc = Jsoup.parseBodyFragment("<table>" + text.trim() + "</table>")

            doc.select("tr").forEach { examElement ->
                val id = EDUDZIENNIK_EXAM_ID.find(examElement.child(0).child(0).attr("href"))
                        ?.get(1)?.crc32() ?: return@forEach
                val topic = examElement.child(0).text().trim()

                val subjectElement = examElement.child(1).child(0)
                val subjectId = EDUDZIENNIK_SUBJECT_ID.find(subjectElement.attr("href"))?.get(1)
                        ?: return@forEach
                val subjectName  = subjectElement.text().trim()
                val subject = data.getSubject(subjectId.crc32(), subjectName)

                val dateString = examElement.child(2).text().trim()
                if (dateString.isBlank()) return@forEach
                val date = Date.fromY_m_d(dateString)

                val lessons = data.app.db.timetableDao().getAllForDateNow(profileId, date)
                val startTime = lessons.firstOrNull { it.displaySubjectId == subject.id }?.displayStartTime

                val eventTypeElement = examElement.child(3).child(0)
                val eventTypeId = EDUDZIENNIK_EVENT_TYPE_ID.find(eventTypeElement.attr("href"))?.get(1)
                        ?: return@forEach
                val eventTypeName = eventTypeElement.text()
                val eventType = data.getEventType(eventTypeId, eventTypeName)

                val eventObject = Event(
                        profileId = profileId,
                        id = id,
                        date = date,
                        time = startTime,
                        topic = topic,
                        color = null,
                        type = eventType.id,
                        teacherId = -1,
                        subjectId = subject.id,
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

            data.toRemove.add(DataRemoveModel.Events.futureExceptTypes(listOf(
                    Event.TYPE_HOMEWORK,
                    Event.TYPE_CLASS_EVENT
            )))

            data.setSyncNext(ENDPOINT_EDUDZIENNIK_WEB_EXAMS, SYNC_ALWAYS)
            onSuccess(ENDPOINT_EDUDZIENNIK_WEB_EXAMS)
        }
    }}
}

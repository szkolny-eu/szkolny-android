/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-29
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.web

import org.jsoup.Jsoup
import pl.szczodrzynski.edziennik.crc32
import pl.szczodrzynski.edziennik.data.api.Regexes.EDUDZIENNIK_HOMEWORK_ID
import pl.szczodrzynski.edziennik.data.api.Regexes.EDUDZIENNIK_SUBJECT_ID
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.ENDPOINT_EDUDZIENNIK_WEB_HOMEWORK
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.EdudziennikWeb
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.get
import pl.szczodrzynski.edziennik.utils.models.Date

class EdudziennikWebHomework(override val data: DataEdudziennik,
                             val onSuccess: () -> Unit) : EdudziennikWeb(data) {
    companion object {
        const val TAG = "EdudziennikWebHomework"
    }

    init { data.profile?.also { profile ->
        webGet(TAG, data.courseStudentEndpoint + "Homework", xhr = true) { text ->
            val doc = Jsoup.parseBodyFragment("<table>" + text.trim() + "</table>")

            if (doc.getElementsByClass("message").text().trim() != "Brak prac domowych") {
                doc.getElementsByTag("tr").forEach { homeworkElement ->
                    val dateElement = homeworkElement.getElementsByClass("date").first().child(0)
                    val id = EDUDZIENNIK_HOMEWORK_ID.find(dateElement.attr("href"))?.get(1)?.crc32()
                            ?: return@forEach
                    val date = Date.fromY_m_d(dateElement.text())

                    val subjectElement = homeworkElement.child(1).child(0)
                    val subjectId = EDUDZIENNIK_SUBJECT_ID.find(subjectElement.attr("href"))?.get(1)
                            ?: return@forEach
                    val subjectName = subjectElement.text()
                    val subject = data.getSubject(subjectId, subjectName)

                    val lessons = data.app.db.timetableDao().getForDateNow(profileId, date)
                    val startTime = lessons.firstOrNull { it.subjectId == subject.id }?.displayStartTime

                    val teacherName = homeworkElement.child(2).text()
                    val teacher = data.getTeacherByFirstLast(teacherName)

                    val topic = homeworkElement.child(4).text()

                    val eventObject = Event(
                            profileId,
                            id,
                            date,
                            startTime,
                            topic,
                            -1,
                            Event.TYPE_HOMEWORK,
                            false,
                            teacher.id,
                            subject.id,
                            data.teamClass?.id ?: -1
                    )

                    data.eventList.add(eventObject)
                    data.metadataList.add(Metadata(
                            profileId,
                            Metadata.TYPE_HOMEWORK,
                            id,
                            profile.empty,
                            profile.empty,
                            System.currentTimeMillis()
                    ))
                }
            }

            data.toRemove.add(DataRemoveModel.Events.futureWithType(Event.TYPE_HOMEWORK))

            data.setSyncNext(ENDPOINT_EDUDZIENNIK_WEB_HOMEWORK, SYNC_ALWAYS)
            onSuccess()
        }
    } ?: onSuccess() }
}

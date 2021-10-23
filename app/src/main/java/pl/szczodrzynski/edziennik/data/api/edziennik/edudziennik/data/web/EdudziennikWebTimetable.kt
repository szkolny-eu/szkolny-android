/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-23
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.web

import org.jsoup.Jsoup
import pl.szczodrzynski.edziennik.data.api.Regexes.EDUDZIENNIK_SUBJECT_ID
import pl.szczodrzynski.edziennik.data.api.Regexes.EDUDZIENNIK_TEACHER_ID
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.ENDPOINT_EDUDZIENNIK_WEB_TIMETABLE
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.EdudziennikWeb
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.db.entity.LessonRange
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.ext.crc32
import pl.szczodrzynski.edziennik.ext.get
import pl.szczodrzynski.edziennik.ext.getString
import pl.szczodrzynski.edziennik.ext.singleOrNull
import pl.szczodrzynski.edziennik.utils.Utils.d
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.edziennik.utils.models.Week

class EdudziennikWebTimetable(override val data: DataEdudziennik,
                              override val lastSync: Long?,
                              val onSuccess: (endpointId: Int) -> Unit
) : EdudziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "EdudziennikWebTimetable"
    }

    init { data.profile?.also { profile ->

        val currentWeekStart = Week.getWeekStart()

        if (Date.getToday().weekDay > 4) {
            currentWeekStart.stepForward(0, 0, 7)
        }

        val getDate = data.arguments?.getString("weekStart") ?: currentWeekStart.stringY_m_d

        val weekStart = Date.fromY_m_d(getDate)
        val weekEnd = weekStart.clone().stepForward(0, 0, 6)

        webGet(TAG, data.timetableEndpoint + "print?date=$getDate") { text ->
            val doc = Jsoup.parse(text)

            val dataDays = mutableListOf<Int>()
            val dataStart = weekStart.clone()
            while (dataStart <= weekEnd) {
                dataDays += dataStart.value
                dataStart.stepForward(0, 0, 1)
            }

            val table = doc.select("#Schedule tbody").first()

            if (table?.text()?.contains("Brak planu lekcji.") == false) {
                table.children().forEach { row ->
                    val rowElements = row.children()

                    val lessonNumber = rowElements[0].text().toInt()

                    val times = rowElements[1].text().split('-')
                    val startTime = Time.fromH_m(times[0].trim())
                    val endTime = Time.fromH_m(times[1].trim())

                    data.lessonRanges.singleOrNull {
                        it.lessonNumber == lessonNumber && it.startTime == startTime && it.endTime == endTime
                    } ?: run {
                        data.lessonRanges.put(lessonNumber, LessonRange(profileId, lessonNumber, startTime, endTime))
                    }

                    rowElements.subList(2, rowElements.size).forEachIndexed { index, lesson ->
                        val course = lesson.select(".course").firstOrNull() ?: return@forEachIndexed
                        val info = course.select("span > span")

                        if (info.isEmpty()) return@forEachIndexed

                        val type = when (course.hasClass("substitute")) {
                            true -> Lesson.TYPE_CHANGE
                            else -> Lesson.TYPE_NORMAL
                        }

                        /* Getting subject */

                        val subjectElement = info[0].child(0)
                        val subjectId = EDUDZIENNIK_SUBJECT_ID.find(subjectElement.attr("href"))?.get(1)
                                ?: return@forEachIndexed
                        val subjectName = subjectElement.text().trim()
                        val subject = data.getSubject(subjectId.crc32(), subjectName)

                        /* Getting teacher */

                        val teacherId = if (info.size >= 2) {
                            val teacherElement = info[1].child(0)
                            val teacherLongId = EDUDZIENNIK_TEACHER_ID.find(teacherElement.attr("href"))?.get(1)
                            val teacherName = teacherElement.text().trim()
                            data.getTeacherByLastFirst(teacherName, teacherLongId).id
                        } else null

                        val lessonObject = Lesson(profileId, -1).also {
                            it.type = type
                            it.date = weekStart.clone().stepForward(0, 0, index)
                            it.lessonNumber = lessonNumber
                            it.startTime = startTime
                            it.endTime = endTime
                            it.subjectId = subject.id
                            it.teacherId = teacherId
                            it.teamId = data.teamClass?.id

                            it.id = it.buildId()
                        }

                        data.lessonList.add(lessonObject)
                        dataDays.remove(lessonObject.date!!.value)

                        if (type != Lesson.TYPE_NORMAL) {
                            val seen = profile.empty || lessonObject.date!! < Date.getToday()

                            data.metadataList.add(Metadata(
                                    profileId,
                                    Metadata.TYPE_LESSON_CHANGE,
                                    lessonObject.id,
                                    seen,
                                    seen
                            ))
                        }
                    }
                }
            }

            for (day in dataDays) {
                val lessonDate = Date.fromValue(day)
                data.lessonList += Lesson(profileId, lessonDate.value.toLong()).apply {
                    type = Lesson.TYPE_NO_LESSONS
                    date = lessonDate
                }
            }

            d(TAG, "Clearing lessons between ${weekStart.stringY_m_d} and ${weekEnd.stringY_m_d} - timetable downloaded for $getDate")

            data.toRemove.add(DataRemoveModel.Timetable.between(weekStart, weekEnd))
            data.setSyncNext(ENDPOINT_EDUDZIENNIK_WEB_TIMETABLE, SYNC_ALWAYS)
            onSuccess(ENDPOINT_EDUDZIENNIK_WEB_TIMETABLE)
        }
    } ?: onSuccess(ENDPOINT_EDUDZIENNIK_WEB_TIMETABLE) }
}

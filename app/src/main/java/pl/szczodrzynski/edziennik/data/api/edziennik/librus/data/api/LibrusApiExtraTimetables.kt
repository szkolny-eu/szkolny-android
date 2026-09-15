/*
 * Copyright (c) Kuba Szczodrzyński 2024-8-27.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_EXTRA_TIMETABLES
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.edziennik.utils.models.Week
import timber.log.Timber

class LibrusApiExtraTimetables(override val data: DataLibrus,
                               override val lastSync: Long?,
                               val onSuccess: (endpointId: Int) -> Unit
) : LibrusApi(data, lastSync) {
    companion object {
        const val TAG = "LibrusApiExtraTimetables"
    }

    init {
        val currentWeekStart = Week.getWeekStart()

        if (Date.getToday().weekDay > 4) {
            currentWeekStart.stepForward(0, 0, 7)
        }

        val getDate = data.arguments?.getString("weekStart") ?: currentWeekStart.stringY_m_d

        val weekStart = Date.fromY_m_d(getDate)
        val weekEnd = weekStart.clone().stepForward(0, 0, 6)

        val dateFrom = weekStart.stringY_m_d
        val dateTo = weekEnd.stringY_m_d

        apiGet(TAG, "Timetables/OtherActivitiesRegister?dateFrom=$dateFrom&dateTo=$dateTo&hideOutdatedEntries=false") { json ->
            val lessons = json.getJsonArray("data")?.asJsonObjectList()

            lessons?.forEach { lesson ->
                val status = lesson.getString("status")
                if (status == "SUBSTITUTED") return@forEach

                val lessonDate = lesson.getString("date")?.let { Date.fromY_m_d(it) } ?: return@forEach
                val startTime = lesson.getString("startTime")?.let { Time.fromH_m(it) } ?: return@forEach
                val endTime = lesson.getString("endTime")?.let { Time.fromH_m(it) } ?: return@forEach
                val title = lesson.getString("title") ?: return@forEach
                val teacherName = lesson.getString("teacherName") ?: ""
                val classroomSymbol = lesson.getJsonObject("classroom")?.getString("symbol")

                val teacher = data.getTeacherByFirstLast(teacherName)
                val subject = data.getSubject(null, title)

                val matchedLessonNumber = findLessonNumber(startTime, endTime)

                val lessonObject = Lesson(profileId, -1).apply {
                    type = Lesson.TYPE_NORMAL
                    date = lessonDate
                    lessonNumber = matchedLessonNumber
                    this.startTime = startTime
                    this.endTime = endTime
                    subjectId = subject.id
                    teacherId = teacher.id
                    classroom = classroomSymbol
                    isExtra = true
                }

                lessonObject.id = lessonObject.buildId()
                lessonObject.ownerId = lessonObject.buildOwnerId()

                data.lessonList.add(lessonObject)
            }

            Timber.d("Clearing extra lessons between $dateFrom and $dateTo - extra timetable downloaded for $getDate")

            data.toRemove.add(DataRemoveModel.Timetable.between(weekStart, weekEnd, isExtra = true))
            data.setSyncNext(ENDPOINT_LIBRUS_API_EXTRA_TIMETABLES, SYNC_ALWAYS)
            onSuccess(ENDPOINT_LIBRUS_API_EXTRA_TIMETABLES)
        }
    }

    private fun findLessonNumber(startTime: Time, endTime: Time): Int {
        var bestNumber = 0
        var bestWeight = -1f

        for (i in 0 until data.lessonRanges.size()) {
            val range = data.lessonRanges.valueAt(i) ?: continue

            val overlapStart = maxOf(timeToMinutes(startTime), timeToMinutes(range.startTime))
            val overlapEnd = minOf(timeToMinutes(endTime), timeToMinutes(range.endTime))
            val overlap = (overlapEnd - overlapStart).coerceAtLeast(0)

            val totalSpan = maxOf(timeToMinutes(range.endTime), timeToMinutes(endTime)) - minOf(timeToMinutes(range.startTime), timeToMinutes(startTime))

            if (totalSpan == 0) continue

            val weight = overlap.toFloat() / totalSpan.toFloat()
            if (weight > bestWeight) {
                bestWeight = weight
                bestNumber = range.lessonNumber
            }
        }

        return bestNumber
    }

    private fun timeToMinutes(time: Time) = time.hour * 60 + time.minute
}

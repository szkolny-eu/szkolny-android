/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-16.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.usos.data.api

import com.google.gson.JsonArray
import pl.szczodrzynski.edziennik.data.api.ERROR_USOS_API_INCOMPLETE_RESPONSE
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.DataUsos
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.ENDPOINT_USOS_API_TIMETABLE
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.data.UsosApi
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.edziennik.utils.models.Week

class UsosApiTimetable(
    override val data: DataUsos,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit,
) : UsosApi(data, lastSync) {
    companion object {
        const val TAG = "UsosApiTimetable"
    }

    init {
        val currentWeekStart = Week.getWeekStart()
        if (Date.getToday().weekDay > 4)
            currentWeekStart.stepForward(0, 0, 7)

        val weekStart = data.arguments
            ?.getString("weekStart")
            ?.let { Date.fromY_m_d(it) }
            ?: currentWeekStart
        val weekEnd = weekStart.clone().stepForward(0, 0, 6)

        apiRequest<JsonArray>(
            tag = TAG,
            service = "tt/user",
            params = mapOf(
                "start" to weekStart.stringY_m_d,
                "days" to 7,
            ),
            fields = listOf(
                "type",
                "start_time",
                "end_time",
                "unit_id",
                "course_id",
                "course_name",
                "lecturer_ids",
                "building_id",
                "room_number",
                "classtype_id",
                "group_number",
            ),
            responseType = ResponseType.ARRAY,
        ) { json, response ->
            if (!processResponse(json, weekStart..weekEnd)) {
                data.error(TAG, ERROR_USOS_API_INCOMPLETE_RESPONSE, response)
                return@apiRequest
            }

            data.toRemove.add(DataRemoveModel.Timetable.between(weekStart, weekEnd))
            data.setSyncNext(ENDPOINT_USOS_API_TIMETABLE, SYNC_ALWAYS)
            onSuccess(ENDPOINT_USOS_API_TIMETABLE)
        }
    }

    private fun processResponse(json: JsonArray, syncRange: ClosedRange<Date>): Boolean {
        val foundDates = mutableSetOf<Date>()

        for (activity in json.asJsonObjectList()) {
            val type = activity.getString("type")
            if (type !in listOf("classgroup", "classgroup2"))
                continue

            val startTime = activity.getString("start_time") ?: continue
            val endTime = activity.getString("end_time") ?: continue
            val unitId = activity.getLong("unit_id", -1)
            val courseName = activity.getLangString("course_name") ?: continue
            val courseId = activity.getString("course_id") ?: continue
            val lecturerIds = activity.getJsonArray("lecturer_ids")?.map { it.asLong }
            val buildingId = activity.getString("building_id")
            val roomNumber = activity.getString("room_number")
            val classTypeId = activity.getString("classtype_id")
            val groupNumber = activity.getString("group_number")

            val lesson = Lesson(profileId, -1).also {
                it.type = Lesson.TYPE_NORMAL
                it.date = Date.fromY_m_d(startTime)
                it.startTime = Time.fromY_m_d_H_m_s(startTime)
                it.endTime = Time.fromY_m_d_H_m_s(endTime)
                it.subjectId = data.getSubject(
                    id = null,
                    name = courseName,
                    shortName = courseId,
                ).id
                it.teacherId = lecturerIds?.firstOrNull() ?: -1L
                it.teamId = unitId
                val groupName = classTypeId?.plus(groupNumber)?.let { s -> "($s)" }
                it.classroom = "Sala $roomNumber / bud. $buildingId ${groupName ?: ""}"
                it.id = it.buildId()
                it.ownerId = it.buildOwnerId()

                it.color = when (classTypeId) {
                    "WYK" -> 0xff0d6091
                    "CW" -> 0xff54306e
                    "LAB" -> 0xff772747
                    "KON" -> 0xff1e5128
                    "^P?SEM" -> 0xff1e5128 // TODO make it regex
                    else -> 0xff08534c
                }.toInt()
            }
            lesson.date?.let { foundDates += it }

            val seen = profile?.empty != false || lesson.date!! < Date.getToday()
            data.lessonList.add(lesson)
            if (lesson.type != Lesson.TYPE_NORMAL)
                data.metadataList += Metadata(
                    profileId,
                    MetadataType.LESSON_CHANGE,
                    lesson.id,
                    seen,
                    seen,
                )
        }

        val notFoundDates = syncRange.asSequence() - foundDates
        for (date in notFoundDates) {
            data.lessonList += Lesson(profileId, date.value.toLong()).also {
                it.type = Lesson.TYPE_NO_LESSONS
                it.date = date
            }
        }
        return true
    }
}

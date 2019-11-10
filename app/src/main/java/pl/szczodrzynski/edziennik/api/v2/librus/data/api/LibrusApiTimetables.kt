/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-10
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.ENDPOINT_LIBRUS_API_TIMETABLES
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.timetable.Lesson
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class LibrusApiTimetables(override val data: DataLibrus,
                          val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiTimetables"
    }

    init {
        apiGet(TAG, "Timetables") { json ->
            json.getJsonObject("Timetable")?.also { timetables ->
                timetables.keySet().forEach { dateStr ->
                    val date = Date.fromY_m_d(dateStr)

                    timetables.getJsonArray(dateStr)?.asJsonObjectList()?.forEach { lesson ->
                        val lessonNumber = lesson.getInt("LessonNo")
                        val startTime = lesson.getString("HourFrom")?.let { Time.fromH_m(it) }
                        val endTime = lesson.getString("HourTo")?.let { Time.fromH_m(it) }
                        val teacherId = lesson.getJsonObject("Teacher")?.getLong("Id")
                        val subjectId = lesson.getJsonObject("Subject")?.getLong("Id")
                        val classId = lesson.getJsonObject("Class")?.getLong("Id")
                        val virtualClassId = lesson.getJsonObject("VirtualClass")?.getLong("Id")
                        val teamId = virtualClassId ?: classId
                        val classroomId = lesson.getJsonObject("Classroom")?.getLong("Id")
                        val classroom = data.classrooms.singleOrNull { it.id == classroomId }?.name

                        val lessonObject = Lesson(profileId).apply {
                            this.date = date
                            this.lessonNumber = lessonNumber
                            this.startTime = startTime
                            this.endTime = endTime
                            this.teacherId = teacherId
                            this.subjectId = subjectId
                            this.teamId = teamId
                            this.classroom = classroom
                        }

                        // TODO add to the database
                    }
                }
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_TIMETABLES, SYNC_ALWAYS)
            onSuccess()
        }
    }
}

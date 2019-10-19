/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-13
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.ENDPOINT_LIBRUS_API_ATTENDANCES
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.edziennik.utils.models.Date

class LibrusApiAttendances(override val data: DataLibrus,
                           val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiAttendances"
    }

    init {
        apiGet(TAG, "Attendances") { json ->
            val attendances = json.getJsonArray("Attendances")

            attendances?.forEach { attendanceEl ->
                val attendance = attendanceEl.asJsonObject

                val id = Utils.strToInt((attendance.getString("Id") ?: return@forEach)
                        .replace("[^\\d.]".toRegex(), "")).toLong()
                val teacherId = attendance.getJsonObject("AddedBy")?.getLong("Id") ?: -1
                val lessonNo = attendance.getInt("LessonNo") ?: return@forEach
                val startTime = data.lessonRanges.get(lessonNo).startTime
                val lessonDate = Date.fromY_m_d(attendance.getString("Date"))
                val subjectId = data.lessonList.singleOrNull {
                    it.weekDay ==  lessonDate.weekDay && it.startTime.value == startTime.value
                }?.subjectId ?: -1
                val semester = attendance.getInt("Semester") ?: return@forEach
                var type = attendance.getJsonObject("Type")?.getInt("Id") ?: return@forEach
                val attendanceType = data.attendanceTypes.get(type)
                val topic = attendanceType.second

                type = when(type) {
                    1 -> Attendance.TYPE_ABSENT
                    2 -> Attendance.TYPE_BELATED
                    3 -> Attendance.TYPE_ABSENT_EXCUSED
                    4 -> Attendance.TYPE_RELEASED
                    else -> Attendance.TYPE_PRESENT
                }

                val attendanceObject = Attendance(
                        profileId,
                        id,
                        teacherId,
                        subjectId,
                        semester,
                        topic,
                        lessonDate,
                        startTime,
                        type
                )

                val addedDate = Date.fromIso(attendance.getString("AddDate") ?: return@forEach)

                data.attendanceList.add(attendanceObject)
                if(type != Attendance.TYPE_PRESENT) {
                    data.metadataList.add(Metadata(
                            profileId,
                            Metadata.TYPE_ATTENDANCE,
                            id,
                            profile?.empty ?: false,
                            profile?.empty ?: false,
                            addedDate
                    ))
                }
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_ATTENDANCES, SYNC_ALWAYS)
            onSuccess()
        }
    }
}

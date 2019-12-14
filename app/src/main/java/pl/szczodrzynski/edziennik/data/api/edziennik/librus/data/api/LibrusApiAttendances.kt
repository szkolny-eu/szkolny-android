/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-13
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import androidx.core.util.isEmpty
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_ATTENDANCES
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
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
        if (data.attendanceTypes.isEmpty()) {
            data.db.attendanceTypeDao().getAllNow(profileId).toSparseArray(data.attendanceTypes) { it.id }
        }

        apiGet(TAG, "Attendances") { json ->
            val attendances = json.getJsonArray("Attendances").asJsonObjectList()

            attendances?.forEach { attendance ->
                val id = Utils.strToInt((attendance.getString("Id") ?: return@forEach)
                        .replace("[^\\d.]".toRegex(), "")).toLong()
                val teacherId = attendance.getJsonObject("AddedBy")?.getLong("Id") ?: -1
                val lessonNo = attendance.getInt("LessonNo") ?: return@forEach
                val startTime = data.lessonRanges.get(lessonNo).startTime
                val lessonDate = Date.fromY_m_d(attendance.getString("Date"))
                val semester = attendance.getInt("Semester") ?: return@forEach
                val type = attendance.getJsonObject("Type")?.getLong("Id") ?: return@forEach
                val typeObject = data.attendanceTypes.get(type)
                val topic = typeObject?.name ?: ""

                val lessonList = data.db.timetableDao().getForDateNow(profileId, lessonDate)
                val subjectId = lessonList.firstOrNull { it.startTime == startTime }?.subjectId ?: -1

                val attendanceObject = Attendance(
                        profileId,
                        id,
                        teacherId,
                        subjectId,
                        semester,
                        topic,
                        lessonDate,
                        startTime,
                        typeObject.type
                )

                val addedDate = Date.fromIso(attendance.getString("AddDate") ?: return@forEach)

                data.attendanceList.add(attendanceObject)
                if(typeObject.type != Attendance.TYPE_PRESENT) {
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

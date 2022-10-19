/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-13
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import androidx.core.util.isEmpty
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_ATTENDANCES
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.utils.models.Date

class LibrusApiAttendances(override val data: DataLibrus,
                           override val lastSync: Long?,
                           val onSuccess: (endpointId: Int) -> Unit
) : LibrusApi(data, lastSync) {
    companion object {
        const val TAG = "LibrusApiAttendances"
    }

    init {
        if (data.attendanceTypes.isEmpty()) {
            data.db.attendanceTypeDao().getAllNow(profileId).toSparseArray(data.attendanceTypes) { it.id }
        }
        if (data.librusLessons.isEmpty()) {
            data.db.librusLessonDao().getAllNow(profileId).toSparseArray(data.librusLessons) { it.lessonId }
        }

        apiGet(TAG, "Attendances") { json ->
            val attendances = json.getJsonArray("Attendances")?.asJsonObjectList()

            attendances?.forEach { attendance ->
                val id = ((attendance.getString("Id") ?: return@forEach)
                        .replace("[^\\d.]".toRegex(), "")).toLong()
                val lessonId = attendance.getJsonObject("Lesson")?.getLong("Id") ?: -1
                val lessonNo = attendance.getInt("LessonNo") ?: return@forEach
                val lessonDate = Date.fromY_m_d(attendance.getString("Date"))
                val teacherId = attendance.getJsonObject("AddedBy")?.getLong("Id")
                val semester = attendance.getInt("Semester") ?: return@forEach

                val typeId = attendance.getJsonObject("Type")?.getLong("Id") ?: return@forEach
                val type = data.attendanceTypes[typeId] ?: null

                val startTime = data.lessonRanges.get(lessonNo)?.startTime

                val lesson = if (lessonId != -1L)
                    data.librusLessons.singleOrNull { it.lessonId == lessonId }
                else null

                val addedDate = Date.fromIso(attendance.getString("AddDate") ?: return@forEach)

                val attendanceObject = Attendance(
                        profileId = profileId,
                        id = id,
                        baseType = type?.baseType ?: Attendance.TYPE_UNKNOWN,
                        typeName = type?.typeName ?: "nieznany rodzaj",
                        typeShort = type?.typeShort ?: "?",
                        typeSymbol = type?.typeSymbol ?: "?",
                        typeColor = type?.typeColor,
                        date = lessonDate,
                        startTime = startTime,
                        semester = semester,
                        teacherId = teacherId ?: lesson?.teacherId ?: -1,
                        subjectId = lesson?.subjectId ?: -1,
                        addedDate = addedDate
                ).also {
                    it.lessonNumber = lessonNo
                }

                data.attendanceList.add(attendanceObject)
                if(type?.baseType != Attendance.TYPE_PRESENT) {
                    data.metadataList.add(Metadata(
                            profileId,
                            MetadataType.ATTENDANCE,
                            id,
                            profile?.empty ?: false || type?.baseType == Attendance.TYPE_PRESENT_CUSTOM || type?.baseType == Attendance.TYPE_UNKNOWN,
                            profile?.empty ?: false || type?.baseType == Attendance.TYPE_PRESENT_CUSTOM || type?.baseType == Attendance.TYPE_UNKNOWN
                    ))
                }
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_ATTENDANCES, SYNC_ALWAYS)
            onSuccess(ENDPOINT_LIBRUS_API_ATTENDANCES)
        }
    }
}

/*
 * Copyright (c) Kacper Ziubryniewicz 2021-2-21
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.VULCAN_HEBE_ENDPOINT_ATTENDANCE
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_HEBE_ATTENDANCE
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanHebe
import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS

class VulcanHebeAttendance(
    override val data: DataVulcan,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit
) : VulcanHebe(data, lastSync) {

    companion object {
        const val TAG = "VulcanHebeAttendance"
    }

    init {
        val semesterNumber = data.studentSemesterNumber
        val startDate = profile?.getSemesterStart(semesterNumber)
        val endDate = profile?.getSemesterEnd(semesterNumber)

        apiGetList(
            TAG,
            VULCAN_HEBE_ENDPOINT_ATTENDANCE,
            HebeFilterType.BY_PUPIL,
            dateFrom = startDate,
            dateTo = endDate,
            lastSync = lastSync
        ) { list, _ ->
            list.forEach { attendance ->
                val id = attendance.getLong("AuxPresenceId") ?: return@forEach
                val type = attendance.getJsonObject("PresenceType") ?: return@forEach
                val baseType = getBaseType(type)
                val typeName = type.getString("Name") ?: return@forEach
                val typeCategoryId = type.getLong("CategoryId") ?: return@forEach
                val typeSymbol = type.getString("Symbol") ?: return@forEach
                val typeShort = when (typeCategoryId.toInt()) {
                    6, 8 -> typeSymbol
                    else -> data.app.attendanceManager.getTypeShort(baseType)
                }
                val typeColor = when (typeCategoryId.toInt()) {
                    1 -> 0xffffffff  // obecność
                    2 -> 0xffffa687  // nieobecność
                    3 -> 0xfffcc150  // nieobecność usprawiedliwiona
                    4 -> 0xffede049  // spóźnienie
                    5 -> 0xffbbdd5f  // spóźnienie usprawiedliwione
                    6 -> 0xffa9c9fd  // nieobecny z przyczyn szkolnych
                    7 -> 0xffddbbe5  // zwolniony
                    8 -> 0xffffffff  // usunięty wpis
                    else -> null
                }?.toInt()
                val date = getDate(attendance, "Day") ?: return@forEach
                val lessonRange = getLessonRange(attendance, "TimeSlot")
                val startTime = lessonRange?.startTime
                val semester = profile?.dateToSemester(date) ?: return@forEach
                val teacherId = attendance.getJsonObject("TeacherPrimary")?.getLong("Id") ?: -1
                val subjectId = attendance.getJsonObject("Subject")?.getLong("Id") ?: -1
                val addedDate = getDateTime(attendance, "DateModify")
                val lessonNumber = lessonRange?.lessonNumber
                val isCounted = attendance.getBoolean("CalculatePresence")
                    ?: (baseType != Attendance.TYPE_RELEASED)

                val attendanceObject = Attendance(
                    profileId = profileId,
                    id = id,
                    baseType = baseType,
                    typeName = typeName,
                    typeShort = typeShort,
                    typeSymbol = typeSymbol,
                    typeColor = typeColor,
                    date = date,
                    startTime = startTime,
                    semester = semester,
                    teacherId = teacherId,
                    subjectId = subjectId,
                    addedDate = addedDate
                ).also {
                    it.lessonTopic = attendance.getString("Topic")
                    it.lessonNumber = lessonNumber
                    it.isCounted = isCounted
                }

                data.attendanceList.add(attendanceObject)
                if (baseType != Attendance.TYPE_PRESENT) {
                    data.metadataList.add(
                        Metadata(
                            profileId,
                            Metadata.TYPE_ATTENDANCE,
                            attendanceObject.id,
                            profile?.empty ?: true
                                    || baseType == Attendance.TYPE_PRESENT_CUSTOM
                                    || baseType == Attendance.TYPE_UNKNOWN,
                            profile?.empty ?: true
                                    || baseType == Attendance.TYPE_PRESENT_CUSTOM
                                    || baseType == Attendance.TYPE_UNKNOWN
                        )
                    )
                }
            }

            data.setSyncNext(ENDPOINT_VULCAN_HEBE_ATTENDANCE, SYNC_ALWAYS)
            onSuccess(ENDPOINT_VULCAN_HEBE_ATTENDANCE)
        }
    }

    fun getBaseType(attendanceType: JsonObject): Int {
        val absent = attendanceType.getBoolean("Absence") ?: false
        val excused = attendanceType.getBoolean("AbsenceJustified") ?: false
        return if (absent) {
            if (excused)
                Attendance.TYPE_ABSENT_EXCUSED
            else
                Attendance.TYPE_ABSENT
        } else {
            val belated = attendanceType.getBoolean("Late") ?: false
            val released = attendanceType.getBoolean("LegalAbsence") ?: false
            val present = attendanceType.getBoolean("Presence") ?: true
            if (belated)
                if (excused)
                    Attendance.TYPE_BELATED_EXCUSED
                else
                    Attendance.TYPE_BELATED
            else if (released)
                Attendance.TYPE_RELEASED
            else if (present)
                if (attendanceType.getInt("CategoryId") != 1)
                    Attendance.TYPE_PRESENT_CUSTOM
                else
                    Attendance.TYPE_PRESENT
            else
                Attendance.TYPE_UNKNOWN
        }
    }
}

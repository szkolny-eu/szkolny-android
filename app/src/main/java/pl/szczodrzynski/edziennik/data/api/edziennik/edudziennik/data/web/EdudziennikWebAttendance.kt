/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-24
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.web

import pl.szczodrzynski.edziennik.data.api.Regexes.EDUDZIENNIK_ATTENDANCE_ENTRIES
import pl.szczodrzynski.edziennik.data.api.Regexes.EDUDZIENNIK_ATTENDANCE_TYPE
import pl.szczodrzynski.edziennik.data.api.Regexes.EDUDZIENNIK_ATTENDANCE_TYPES
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.ENDPOINT_EDUDZIENNIK_WEB_ATTENDANCE
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.EdudziennikWeb
import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.ext.crc32
import pl.szczodrzynski.edziennik.ext.get
import pl.szczodrzynski.edziennik.ext.singleOrNull
import pl.szczodrzynski.edziennik.utils.models.Date
import java.util.*

class EdudziennikWebAttendance(override val data: DataEdudziennik,
                               override val lastSync: Long?,
                               val onSuccess: (endpointId: Int) -> Unit
) : EdudziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "EdudziennikWebAttendance"
    }

    private var requestSemester: Int? = null

    init {
        if (profile?.empty == true && data.currentSemester == 2) requestSemester = 1
        getAttendances()
    }

    private fun getAttendances() { data.profile?.also { profile ->
        webGet(TAG, data.studentEndpoint + "Presence", semester = requestSemester) { text ->

            val attendanceTypes = EDUDZIENNIK_ATTENDANCE_TYPES.find(text)?.get(1)?.split(',')?.map {
                val type = EDUDZIENNIK_ATTENDANCE_TYPE.find(it.trim())
                val symbol = type?.get(1)?.trim() ?: "?"
                val name = type?.get(2)?.trim() ?: "nieznany rodzaj"
                return@map Triple(
                        symbol,
                        name,
                        when (name.toLowerCase(Locale.ROOT)) {
                            "obecność" -> Attendance.TYPE_PRESENT
                            "nieobecność" -> Attendance.TYPE_ABSENT
                            "spóźnienie" -> Attendance.TYPE_BELATED
                            "nieobecność usprawiedliwiona" -> Attendance.TYPE_ABSENT_EXCUSED
                            "dzień wolny" -> Attendance.TYPE_DAY_FREE
                            "brak zajęć" -> Attendance.TYPE_DAY_FREE
                            "oddelegowany" -> Attendance.TYPE_RELEASED
                            else -> Attendance.TYPE_UNKNOWN
                        }
                )
            } ?: emptyList()

            EDUDZIENNIK_ATTENDANCE_ENTRIES.findAll(text).forEach { attendanceElement ->
                val date = Date.fromY_m_d(attendanceElement[1])
                val lessonNumber = attendanceElement[2].toInt()
                val attendanceSymbol = attendanceElement[3]

                val lessons = data.app.db.timetableDao().getAllForDateNow(profileId, date)
                val lesson = lessons.firstOrNull { it.lessonNumber == lessonNumber }

                val id = "${date.stringY_m_d}:$lessonNumber:$attendanceSymbol".crc32()

                val (typeSymbol, typeName, baseType) = attendanceTypes.firstOrNull { (symbol, _, _) -> symbol == attendanceSymbol }
                        ?: return@forEach

                val startTime = data.lessonRanges.singleOrNull { it.lessonNumber == lessonNumber }?.startTime
                        ?: return@forEach

                val attendanceObject = Attendance(
                        profileId = profileId,
                        id = id,
                        baseType = baseType,
                        typeName = typeName,
                        typeShort = data.app.attendanceManager.getTypeShort(baseType),
                        typeSymbol = typeSymbol,
                        typeColor = null,
                        date = date,
                        startTime = lesson?.displayStartTime ?: startTime,
                        semester = profile.currentSemester,
                        teacherId = lesson?.displayTeacherId ?: -1,
                        subjectId = lesson?.displaySubjectId ?: -1
                ).also {
                    it.lessonNumber = lessonNumber
                }

                data.attendanceList.add(attendanceObject)
                if (baseType != Attendance.TYPE_PRESENT) {
                    data.metadataList.add(Metadata(
                            profileId,
                            Metadata.TYPE_ATTENDANCE,
                            id,
                            profile.empty || baseType == Attendance.TYPE_PRESENT_CUSTOM || baseType == Attendance.TYPE_UNKNOWN,
                            profile.empty || baseType == Attendance.TYPE_PRESENT_CUSTOM || baseType == Attendance.TYPE_UNKNOWN
                    ))
                }
            }

            if (profile.empty && requestSemester == 1 && data.currentSemester == 2) {
                requestSemester = null
                getAttendances()
            } else {
                data.setSyncNext(ENDPOINT_EDUDZIENNIK_WEB_ATTENDANCE, SYNC_ALWAYS)
                onSuccess(ENDPOINT_EDUDZIENNIK_WEB_ATTENDANCE)
            }
        }
    } ?: onSuccess(ENDPOINT_EDUDZIENNIK_WEB_ATTENDANCE) }
}

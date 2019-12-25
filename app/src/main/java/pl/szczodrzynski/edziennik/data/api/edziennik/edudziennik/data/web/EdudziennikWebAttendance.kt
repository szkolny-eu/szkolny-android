/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-24
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.web

import pl.szczodrzynski.edziennik.crc32
import pl.szczodrzynski.edziennik.data.api.Regexes.EDUDZIENNIK_ATTENDANCE_ENTRIES
import pl.szczodrzynski.edziennik.data.api.Regexes.EDUDZIENNIK_ATTENDANCE_TYPE
import pl.szczodrzynski.edziennik.data.api.Regexes.EDUDZIENNIK_ATTENDANCE_TYPES
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.ENDPOINT_EDUDZIENNIK_WEB_ATTENDANCE
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.EdudziennikWeb
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.get
import pl.szczodrzynski.edziennik.singleOrNull
import pl.szczodrzynski.edziennik.utils.models.Date
import java.util.*

class EdudziennikWebAttendance(override val data: DataEdudziennik,
                               val onSuccess: () -> Unit) : EdudziennikWeb(data) {
    companion object {
        private const val TAG = "EdudziennikWebAttendance"
    }

    init { data.profile?.also { profile ->
        webGet(TAG, data.studentEndpoint + "Presence") { text ->

            val attendanceTypes = EDUDZIENNIK_ATTENDANCE_TYPES.find(text)?.get(1)?.split(',')?.map {
                val type = EDUDZIENNIK_ATTENDANCE_TYPE.find(it.trim())
                val symbol = type?.get(1)?.trim()
                val name = type?.get(2)?.trim()
                return@map Triple(
                        symbol,
                        name,
                        when (name?.toLowerCase(Locale.ROOT)) {
                            "obecność" -> Attendance.TYPE_PRESENT
                            "nieobecność" -> Attendance.TYPE_ABSENT
                            "spóźnienie" -> Attendance.TYPE_BELATED
                            "nieobecność usprawiedliwiona" -> Attendance.TYPE_ABSENT_EXCUSED
                            "dzień wolny" -> Attendance.TYPE_DAY_FREE
                            "brak zajęć" -> Attendance.TYPE_DAY_FREE
                            "oddelegowany" -> Attendance.TYPE_RELEASED
                            else -> Attendance.TYPE_CUSTOM
                        }
                )
            } ?: emptyList()

            EDUDZIENNIK_ATTENDANCE_ENTRIES.findAll(text).forEach { attendanceElement ->
                val date = Date.fromY_m_d(attendanceElement[1])
                val lessonNumber = attendanceElement[2].toInt()
                val attendanceSymbol = attendanceElement[3]

                val lessons = data.app.db.timetableDao().getForDateNow(profileId, date)
                val lesson = lessons.firstOrNull { it.lessonNumber == lessonNumber }

                val id = "${date.stringY_m_d}:$lessonNumber:$attendanceSymbol".crc32()

                val (_, name, type) = attendanceTypes.firstOrNull { (symbol, _, _) -> symbol == attendanceSymbol }
                        ?: return@forEach

                val startTime = data.lessonRanges.singleOrNull { it.lessonNumber == lessonNumber }?.startTime
                        ?: return@forEach

                val attendanceObject = Attendance(
                        profileId,
                        id,
                        lesson?.displayTeacherId ?: -1,
                        lesson?.displaySubjectId ?: -1,
                        profile.currentSemester,
                        name,
                        date,
                        lesson?.displayStartTime ?: startTime,
                        type
                )

                data.attendanceList.add(attendanceObject)
                data.metadataList.add(Metadata(
                        profileId,
                        Metadata.TYPE_ATTENDANCE,
                        id,
                        profile.empty,
                        profile.empty,
                        System.currentTimeMillis()
                ))
            }

            data.setSyncNext(ENDPOINT_EDUDZIENNIK_WEB_ATTENDANCE, SYNC_ALWAYS)
            onSuccess()
        }
    } ?: onSuccess() }
}

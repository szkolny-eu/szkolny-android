/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-28.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.web

import pl.szczodrzynski.edziennik.crc16
import pl.szczodrzynski.edziennik.data.api.ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA
import pl.szczodrzynski.edziennik.data.api.IDZIENNIK_WEB_ATTENDANCE
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.ENDPOINT_IDZIENNIK_WEB_ATTENDANCE
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.IdziennikWeb
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.data.db.entity.Attendance.Companion.TYPE_ABSENT
import pl.szczodrzynski.edziennik.data.db.entity.Attendance.Companion.TYPE_ABSENT_EXCUSED
import pl.szczodrzynski.edziennik.data.db.entity.Attendance.Companion.TYPE_BELATED
import pl.szczodrzynski.edziennik.data.db.entity.Attendance.Companion.TYPE_PRESENT
import pl.szczodrzynski.edziennik.data.db.entity.Attendance.Companion.TYPE_PRESENT_CUSTOM
import pl.szczodrzynski.edziennik.data.db.entity.Attendance.Companion.TYPE_RELEASED
import pl.szczodrzynski.edziennik.data.db.entity.Attendance.Companion.TYPE_UNKNOWN
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.getInt
import pl.szczodrzynski.edziennik.getJsonObject
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class IdziennikWebAttendance(override val data: DataIdziennik,
                             override val lastSync: Long?,
                             val onSuccess: (endpointId: Int) -> Unit
) : IdziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "IdziennikWebAttendance"
    }

    private var attendanceYear = Date.getToday().year
    private var attendanceMonth = Date.getToday().month
    private var attendancePrevMonthChecked = false

    init {
        getAttendance()
    }

    private fun getAttendance() {
        webApiGet(TAG, IDZIENNIK_WEB_ATTENDANCE, mapOf(
                "idPozDziennika" to data.registerId,
                "mc" to attendanceMonth,
                "rok" to attendanceYear,
                "dataTygodnia" to ""
        )) { result ->
            val json = result.getJsonObject("d") ?: run {
                data.error(ApiError(TAG, ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA)
                        .withApiResponse(result))
                return@webApiGet
            }

            for (jAttendanceEl in json.getAsJsonArray("Obecnosci")) {
                val jAttendance = jAttendanceEl.asJsonObject
                // jAttendance
                val type = jAttendance.get("TypObecnosci").asInt

                // skip "zajęcia nie odbyły się" and "Ferie"
                if (type == 5 || type == 7)
                    continue

                val date = Date.fromY_m_d(jAttendance.get("Data").asString)
                val time = Time.fromH_m(jAttendance.get("OdDoGodziny").asString)
                if (date.combineWith(time) > System.currentTimeMillis())
                    continue

                val id = jAttendance.get("IdLesson").asString.crc16().toLong()
                val rSubject = data.getSubject(jAttendance.get("Przedmiot").asString, jAttendance.get("IdPrzedmiot").asLong, "")
                val rTeacher = data.getTeacherByFDotSpaceLast(jAttendance.get("PrzedmiotNauczyciel").asString)

                var baseType = TYPE_UNKNOWN
                var typeName = "nieznany rodzaj"
                var typeSymbol: String? = null
                var typeColor: Long? = null

                /* https://iuczniowie.progman.pl/idziennik/mod_panelRodzica/obecnosci/obecnosciUcznia_lmt637231494660000000.js */
                /* https://iuczniowie.progman.pl/idziennik/mod_panelRodzica/obecnosci/obecnosci_lmt637231494660000000.css */
                when (type) {
                    1 -> {
                        baseType = TYPE_ABSENT_EXCUSED
                        typeName = "nieobecność usprawiedliwiona"
                        typeColor = 0xffffe099
                    }
                    2 -> {
                        baseType = TYPE_BELATED
                        typeName = "spóźnienie"
                        typeColor = 0xffffffaa
                    }
                    3 -> {
                        baseType = TYPE_ABSENT
                        typeName = "nieobecność nieusprawiedliwiona"
                        typeColor = 0xffffad99
                    }
                    4, 9 -> {
                        baseType = TYPE_RELEASED
                        if (type == 4) {
                            typeName = "zwolnienie"
                            typeColor = 0xffa8beff
                        }
                        if (type == 9) {
                            typeName = "zwolniony / obecny"
                            typeSymbol = "zb"
                            typeColor = 0xffff69b4
                        }
                    }
                    8 -> {
                        baseType = TYPE_PRESENT_CUSTOM
                        typeName = "wycieczka"
                        typeSymbol = "w"
                        typeColor = null
                    }
                    0 -> {
                        baseType = TYPE_PRESENT
                        typeName = "obecny"
                        typeColor = 0xffccffcc
                    }
                }

                val semester = profile?.dateToSemester(date) ?: 1

                val attendanceObject = Attendance(
                        profileId = profileId,
                        id = id,
                        baseType = baseType,
                        typeName = typeName,
                        typeShort = typeSymbol ?: data.app.attendanceManager.getTypeShort(baseType),
                        typeSymbol = typeSymbol ?: data.app.attendanceManager.getTypeShort(baseType),
                        typeColor = typeColor?.toInt(),
                        date = date,
                        startTime = time,
                        semester = semester,
                        teacherId = rTeacher.id,
                        subjectId = rSubject.id
                ).also {
                    it.lessonTopic = jAttendance.getString("PrzedmiotTemat")
                    it.lessonNumber = jAttendance.getInt("Godzina")
                }

                data.attendanceList.add(attendanceObject)
                if (attendanceObject.baseType != TYPE_PRESENT) {
                    data.metadataList.add(Metadata(
                            profileId,
                            Metadata.TYPE_ATTENDANCE,
                            attendanceObject.id,
                            profile?.empty ?: false || baseType == TYPE_PRESENT_CUSTOM || baseType == TYPE_UNKNOWN,
                            profile?.empty ?: false || baseType == TYPE_PRESENT_CUSTOM || baseType == TYPE_UNKNOWN
                    ))
                }
            }

            val attendanceDateValue = attendanceYear * 10000 + attendanceMonth * 100
            if (profile?.empty == true && attendanceDateValue > profile?.getSemesterStart(1)?.value ?: 99999999) {
                attendancePrevMonthChecked = true // do not need to check prev month later
                attendanceMonth--
                if (attendanceMonth < 1) {
                    attendanceMonth = 12
                    attendanceYear--
                }
                getAttendance()
            } else if (!attendancePrevMonthChecked /* get also the previous month */) {
                attendanceMonth--
                if (attendanceMonth < 1) {
                    attendanceMonth = 12
                    attendanceYear--
                }
                attendancePrevMonthChecked = true
                getAttendance()
            } else {
                data.setSyncNext(ENDPOINT_IDZIENNIK_WEB_ATTENDANCE, SYNC_ALWAYS)
                onSuccess(ENDPOINT_IDZIENNIK_WEB_ATTENDANCE)
            }
        }
    }
}

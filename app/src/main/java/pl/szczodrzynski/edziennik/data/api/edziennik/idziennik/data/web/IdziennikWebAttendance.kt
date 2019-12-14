/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-28.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.web

import pl.szczodrzynski.edziennik.data.api.ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA
import pl.szczodrzynski.edziennik.data.api.IDZIENNIK_WEB_ATTENDANCE
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.ENDPOINT_IDZIENNIK_WEB_ATTENDANCE
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.IdziennikWeb
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.crc16
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance
import pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance.*
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.getJsonObject
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class IdziennikWebAttendance(override val data: DataIdziennik,
                          val onSuccess: () -> Unit) : IdziennikWeb(data) {
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
                val attendanceTypeIdziennik = jAttendance.get("TypObecnosci").asInt
                if (attendanceTypeIdziennik == 5 || attendanceTypeIdziennik == 7)
                    continue
                val attendanceDate = Date.fromY_m_d(jAttendance.get("Data").asString)
                val attendanceTime = Time.fromH_m(jAttendance.get("OdDoGodziny").asString)
                if (attendanceDate.combineWith(attendanceTime) > System.currentTimeMillis())
                    continue

                val attendanceId = jAttendance.get("IdLesson").asString.crc16().toLong()
                val rSubject = data.getSubject(jAttendance.get("Przedmiot").asString, jAttendance.get("IdPrzedmiot").asLong, "")
                val rTeacher = data.getTeacherByFDotSpaceLast(jAttendance.get("PrzedmiotNauczyciel").asString)

                var attendanceName = "obecność"
                var attendanceType = Attendance.TYPE_CUSTOM

                when (attendanceTypeIdziennik) {
                    1 /* nieobecność usprawiedliwiona */ -> {
                        attendanceName = "nieobecność usprawiedliwiona"
                        attendanceType = TYPE_ABSENT_EXCUSED
                    }
                    2 /* spóźnienie */ -> {
                        attendanceName = "spóźnienie"
                        attendanceType = TYPE_BELATED
                    }
                    3 /* nieobecność nieusprawiedliwiona */ -> {
                        attendanceName = "nieobecność nieusprawiedliwiona"
                        attendanceType = TYPE_ABSENT
                    }
                    4 /* zwolnienie */, 9 /* zwolniony / obecny */ -> {
                        attendanceType = TYPE_RELEASED
                        if (attendanceTypeIdziennik == 4)
                            attendanceName = "zwolnienie"
                        if (attendanceTypeIdziennik == 9)
                            attendanceName = "zwolnienie / obecność"
                    }
                    0 /* obecny */, 8 /* Wycieczka */ -> {
                        attendanceType = TYPE_PRESENT
                        if (attendanceTypeIdziennik == 8)
                            attendanceName = "wycieczka"
                    }
                }

                val semester = profile?.dateToSemester(attendanceDate) ?: 1

                val attendanceObject = Attendance(
                        profileId,
                        attendanceId,
                        rTeacher.id,
                        rSubject.id,
                        semester,
                        attendanceName,
                        attendanceDate,
                        attendanceTime,
                        attendanceType
                )

                data.attendanceList.add(attendanceObject)
                if (attendanceObject.type != TYPE_PRESENT) {
                    data.metadataList.add(Metadata(
                            profileId,
                            Metadata.TYPE_ATTENDANCE,
                            attendanceObject.id,
                            profile?.empty ?: false,
                            profile?.empty ?: false,
                            System.currentTimeMillis()
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
                onSuccess()
            }
        }
    }
}

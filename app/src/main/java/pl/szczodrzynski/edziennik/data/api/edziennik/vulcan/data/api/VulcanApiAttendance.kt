package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.api

import androidx.core.util.isEmpty
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.VULCAN_API_ENDPOINT_ATTENDANCE
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_API_ATTENDANCE
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanApi
import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.data.db.entity.Attendance.Companion.TYPE_PRESENT
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.utils.models.Date

class VulcanApiAttendance(override val data: DataVulcan,
                          override val lastSync: Long?,
                          val onSuccess: (endpointId: Int) -> Unit
) : VulcanApi(data, lastSync) {
    companion object {
        const val TAG = "VulcanApiAttendance"
    }

    init { data.profile?.also { profile ->
        if (data.attendanceTypes.isEmpty()) {
            data.db.attendanceTypeDao().getAllNow(profileId).toSparseArray(data.attendanceTypes) { it.id }
        }

        val startDate: String = profile.getSemesterStart(profile.currentSemester).stringY_m_d
        val endDate: String = profile.getSemesterEnd(profile.currentSemester).stringY_m_d

        apiGet(TAG, VULCAN_API_ENDPOINT_ATTENDANCE, parameters = mapOf(
                "DataPoczatkowa" to startDate,
                "DataKoncowa" to endDate,
                "IdOddzial" to data.studentClassId,
                "IdUczen" to data.studentId,
                "IdOkresKlasyfikacyjny" to data.studentSemesterId
        )) { json, _ ->
            json.getJsonObject("Data")?.getJsonArray("Frekwencje")?.forEach { attendanceEl ->
                val attendance = attendanceEl.asJsonObject

                val type = data.attendanceTypes.get(attendance.getLong("IdKategoria") ?: return@forEach)
                        ?: return@forEach

                val id = (attendance.getInt("Dzien") ?: 0) + (attendance.getInt("Numer") ?: 0)

                val lessonDateMillis = Date.fromY_m_d(attendance.getString("DzienTekst")).inMillis
                val lessonDate = Date.fromMillis(lessonDateMillis)
                val startTime = data.lessonRanges.get(attendance.getInt("Numer") ?: 0)?.startTime

                val lessonSemester = profile.dateToSemester(lessonDate)

                val attendanceObject = Attendance(
                        profileId = profileId,
                        id = id.toLong(),
                        baseType = type.baseType,
                        typeName = type.typeName,
                        typeShort = type.typeShort,
                        typeSymbol = type.typeSymbol,
                        typeColor = type.typeColor,
                        date = lessonDate,
                        startTime = startTime,
                        semester = lessonSemester,
                        teacherId = -1,
                        subjectId = attendance.getLong("IdPrzedmiot") ?: -1,
                        addedDate = lessonDate.combineWith(startTime)
                ).also {
                    it.lessonNumber = attendance.getInt("Numer")
                }

                data.attendanceList.add(attendanceObject)
                if (type.baseType != TYPE_PRESENT) {
                    data.metadataList.add(Metadata(
                            profileId,
                            Metadata.TYPE_ATTENDANCE,
                            attendanceObject.id,
                            profile.empty || type.baseType == Attendance.TYPE_PRESENT_CUSTOM || type.baseType == Attendance.TYPE_UNKNOWN,
                            profile.empty || type.baseType == Attendance.TYPE_PRESENT_CUSTOM || type.baseType == Attendance.TYPE_UNKNOWN
                    ))
                }
            }

            data.setSyncNext(ENDPOINT_VULCAN_API_ATTENDANCE, SYNC_ALWAYS)
            onSuccess(ENDPOINT_VULCAN_API_ATTENDANCE)
        }
    } ?: onSuccess(ENDPOINT_VULCAN_API_ATTENDANCE) }
}

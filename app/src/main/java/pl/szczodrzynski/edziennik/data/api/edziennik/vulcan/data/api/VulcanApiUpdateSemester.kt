/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-1.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.ERROR_NO_STUDENTS_IN_ACCOUNT
import pl.szczodrzynski.edziennik.data.api.VULCAN_API_ENDPOINT_STUDENT_LIST
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_API_UPDATE_SEMESTER
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanApi
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.utils.models.Date

class VulcanApiUpdateSemester(override val data: DataVulcan,
                              override val lastSync: Long?,
                              val onSuccess: (endpointId: Int) -> Unit
) : VulcanApi(data, lastSync) {
    companion object {
        const val TAG = "VulcanApiUpdateSemester"
    }

    init { data.profile?.also { profile ->
        apiGet(TAG, VULCAN_API_ENDPOINT_STUDENT_LIST, baseUrl = true) { json, response ->
            val students = json.getJsonArray("Data")

            if (students == null || students.isEmpty()) {
                data.error(ApiError(TAG, ERROR_NO_STUDENTS_IN_ACCOUNT)
                        .withResponse(response)
                        .withApiResponse(json))
                return@apiGet
            }

            students.asJsonObjectList().firstOrNull {
                it.getInt("Id") == data.studentId
            }?.let { student ->
                val studentClassId = student.getInt("IdOddzial") ?: return@let
                val studentClassName = student.getString("OkresPoziom").toString() + (student.getString("OddzialSymbol") ?: return@let)
                val studentSemesterId = student.getInt("IdOkresKlasyfikacyjny") ?: return@let

                val currentSemesterStartDate = student.getLong("OkresDataOd") ?: return@let
                val currentSemesterEndDate = (student.getLong("OkresDataDo") ?: return@let) + 86400
                val studentSemesterNumber = student.getInt("OkresNumer") ?: return@let

                var dateSemester1Start: Date? = null
                var dateSemester2Start: Date? = null
                var dateYearEnd: Date? = null
                when (studentSemesterNumber) {
                    1 -> {
                        dateSemester1Start = Date.fromMillis(currentSemesterStartDate * 1000)
                        dateSemester2Start = Date.fromMillis(currentSemesterEndDate * 1000)
                    }
                    2 -> {
                        dateSemester2Start = Date.fromMillis(currentSemesterStartDate * 1000)
                        dateYearEnd = Date.fromMillis(currentSemesterEndDate * 1000)
                    }
                }

                data.studentClassId = studentClassId
                data.studentSemesterId = studentSemesterId
                data.studentSemesterNumber = studentSemesterNumber
                data.currentSemesterEndDate = currentSemesterEndDate
                profile.studentClassName = studentClassName
                dateSemester1Start?.let {
                    profile.dateSemester1Start = it
                    profile.studentSchoolYearStart = it.year
                }
                dateSemester2Start?.let { profile.dateSemester2Start = it }
                dateYearEnd?.let { profile.dateYearEnd = it }
            }

            data.setSyncNext(ENDPOINT_VULCAN_API_UPDATE_SEMESTER, if (data.studentSemesterNumber == 2) 7*DAY else 2*DAY)
            onSuccess(ENDPOINT_VULCAN_API_UPDATE_SEMESTER)
        }
    } ?: onSuccess(ENDPOINT_VULCAN_API_UPDATE_SEMESTER) }
}

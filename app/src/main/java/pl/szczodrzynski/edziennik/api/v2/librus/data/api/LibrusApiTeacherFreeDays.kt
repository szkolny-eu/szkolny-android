/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-4.
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.ENDPOINT_LIBRUS_API_TEACHER_FREE_DAYS
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.teachers.TeacherAbsence
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class LibrusApiTeacherFreeDays(override val data: DataLibrus,
                               val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiTeacherFreeDays"
    }

    init {
        apiGet(TAG, "TeacherFreeDays") { json ->
            val teacherAbsences = json.getJsonArray("TeacherFreeDays")

            teacherAbsences?.forEach { teacherAbsenceEl ->
                val teacherAbsence = teacherAbsenceEl.asJsonObject

                val id = teacherAbsence.getLong("Id") ?: return@forEach
                val teacherId = teacherAbsence.getJsonObject("Teacher")?.getLong("Id")
                        ?: return@forEach
                val type = teacherAbsence.getJsonObject("Type").getLong("Id") ?: return@forEach
                val dateFrom = Date.fromY_m_d(teacherAbsence.getString("DateFrom"))
                val dateTo = Date.fromY_m_d(teacherAbsence.getString("DateTo"))
                val timeFrom = teacherAbsence.getString("TimeFrom")?.let { Time.fromH_m_s(it) }
                val timeTo = teacherAbsence.getString("TimeTo")?.let { Time.fromH_m_s(it) }

                val teacherAbsenceObject = TeacherAbsence(
                        profileId,
                        id,
                        teacherId,
                        type,
                        dateFrom,
                        dateTo,
                        timeFrom,
                        timeTo
                )

                data.teacherAbsenceList.add(teacherAbsenceObject)
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_TEACHER_FREE_DAYS, SYNC_ALWAYS)
            onSuccess()
        }
    }
}

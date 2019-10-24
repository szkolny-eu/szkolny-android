/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-4.
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.api

import androidx.core.util.isEmpty
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_AGENDA
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.ENDPOINT_LIBRUS_API_TEACHER_FREE_DAYS
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.data.db.modules.teachers.TeacherAbsence
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class LibrusApiTeacherFreeDays(override val data: DataLibrus,
                               val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiTeacherFreeDays"
    }

    init {
        if (data.teacherAbsenceTypes.isEmpty()) {
            data.db.teacherAbsenceTypeDao().getAllNow(profileId).toSparseArray(data.teacherAbsenceTypes) { it.id }
        }

        apiGet(TAG, "TeacherFreeDays") { json ->
            val teacherAbsences = json.getJsonArray("TeacherFreeDays").asJsonObjectList()

            teacherAbsences?.forEach { teacherAbsence ->
                val id = teacherAbsence.getLong("Id") ?: return@forEach
                val teacherId = teacherAbsence.getJsonObject("Teacher")?.getLong("Id")
                        ?: return@forEach
                val type = teacherAbsence.getJsonObject("Type").getLong("Id") ?: return@forEach
                val name = data.teacherAbsenceTypes.singleOrNull { it.id == type }?.name
                val dateFrom = Date.fromY_m_d(teacherAbsence.getString("DateFrom"))
                val dateTo = Date.fromY_m_d(teacherAbsence.getString("DateTo"))
                val timeFrom = teacherAbsence.getString("TimeFrom")?.let { Time.fromH_m_s(it) }
                val timeTo = teacherAbsence.getString("TimeTo")?.let { Time.fromH_m_s(it) }

                val teacherAbsenceObject = TeacherAbsence(
                        profileId,
                        id,
                        teacherId,
                        type,
                        name,
                        dateFrom,
                        dateTo,
                        timeFrom,
                        timeTo
                )

                data.teacherAbsenceList.add(teacherAbsenceObject)
                data.metadataList.add(Metadata(
                        profileId,
                        Metadata.TYPE_TEACHER_ABSENCE,
                        id,
                        profile?.empty ?: false,
                        profile?.empty ?: false,
                        System.currentTimeMillis()
                ))
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_TEACHER_FREE_DAYS, 6*HOUR, DRAWER_ITEM_AGENDA)
            onSuccess()
        }
    }
}

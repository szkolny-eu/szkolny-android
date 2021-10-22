/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-4.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import androidx.core.util.isEmpty
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_AGENDA
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_TEACHER_FREE_DAYS
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.TeacherAbsence
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class LibrusApiTeacherFreeDays(override val data: DataLibrus,
                               override val lastSync: Long?,
                               val onSuccess: (endpointId: Int) -> Unit
) : LibrusApi(data, lastSync) {
    companion object {
        const val TAG = "LibrusApiTeacherFreeDays"
    }

    init {
        if (data.teacherAbsenceTypes.isEmpty()) {
            data.db.teacherAbsenceTypeDao().getAllNow(profileId).toSparseArray(data.teacherAbsenceTypes) { it.id }
        }

        apiGet(TAG, "TeacherFreeDays") { json ->
            val teacherAbsences = json.getJsonArray("TeacherFreeDays")?.asJsonObjectList()

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
                        profileId = profileId,
                        id = id,
                        type = type,
                        name = name,
                        dateFrom = dateFrom,
                        dateTo = dateTo,
                        timeFrom = timeFrom,
                        timeTo = timeTo,
                        teacherId = teacherId
                )

                data.teacherAbsenceList.add(teacherAbsenceObject)
                data.metadataList.add(Metadata(
                        profileId,
                        Metadata.TYPE_TEACHER_ABSENCE,
                        id,
                        true,
                        profile?.empty ?: false
                ))
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_TEACHER_FREE_DAYS, 6* HOUR, DRAWER_ITEM_AGENDA)
            onSuccess(ENDPOINT_LIBRUS_API_TEACHER_FREE_DAYS)
        }
    }
}

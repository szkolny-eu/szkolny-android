/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-4.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import androidx.core.util.isEmpty
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_TEACHER_FREE_DAYS
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.TeacherAbsence
import pl.szczodrzynski.edziennik.data.db.enums.FeatureType
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
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
                val dateFrom = Date.fromY_m_d(teacherAbsence.getString("DateFrom"))
                val dateTo = Date.fromY_m_d(teacherAbsence.getString("DateTo"))
                val timeFrom = teacherAbsence.getString("TimeFrom")?.let { Time.fromH_m_s(it) }
                val timeTo = teacherAbsence.getString("TimeTo")?.let { Time.fromH_m_s(it) }

                val teacherAbsenceObject = TeacherAbsence(
                        profileId = profileId,
                        id = id,
                        type = -1L,
                        name = null,
                        dateFrom = dateFrom,
                        dateTo = dateTo,
                        timeFrom = timeFrom,
                        timeTo = timeTo,
                        teacherId = teacherId
                )

                data.teacherAbsenceList.add(teacherAbsenceObject)
                data.metadataList.add(Metadata(
                        profileId,
                        MetadataType.TEACHER_ABSENCE,
                        id,
                        true,
                        profile?.empty ?: false
                ))
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_TEACHER_FREE_DAYS, 6* HOUR, FeatureType.AGENDA)
            onSuccess(ENDPOINT_LIBRUS_API_TEACHER_FREE_DAYS)
        }
    }
}

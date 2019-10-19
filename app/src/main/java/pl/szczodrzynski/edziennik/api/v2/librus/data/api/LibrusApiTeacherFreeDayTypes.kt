/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-19
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.api

import pl.szczodrzynski.edziennik.DAY
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.ENDPOINT_LIBRUS_API_TEACHER_FREE_DAY_TYPES
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.teachers.TeacherAbsenceType
import pl.szczodrzynski.edziennik.getJsonArray
import pl.szczodrzynski.edziennik.getLong
import pl.szczodrzynski.edziennik.getString

class LibrusApiTeacherFreeDayTypes(override val data: DataLibrus,
                                   val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiTeacherFreeDayTypes"
    }

    init {
        apiGet(TAG, "TeacherFreeDays/Types") { json ->
            val teacherAbsenceTypes = json.getJsonArray("Types")

            teacherAbsenceTypes?.forEach { teacherAbsenceTypeEl ->
                val teacherAbsenceType = teacherAbsenceTypeEl.asJsonObject

                val id = teacherAbsenceType.getLong("Id") ?: return@forEach
                val name = teacherAbsenceType.getString("Name") ?: return@forEach

                val teacherAbsenceTypeObject = TeacherAbsenceType(
                        profileId,
                        id,
                        name
                )

                data.teacherAbsenceTypes.put(id, teacherAbsenceTypeObject)
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_TEACHER_FREE_DAY_TYPES, 4 * DAY)
            onSuccess()
        }
    }
}

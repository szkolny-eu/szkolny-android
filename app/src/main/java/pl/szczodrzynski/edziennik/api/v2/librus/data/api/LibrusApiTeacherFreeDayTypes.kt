/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-19
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.ENDPOINT_LIBRUS_API_TEACHER_FREE_DAY_TYPES
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.modules.teachers.TeacherAbsenceType

class LibrusApiTeacherFreeDayTypes(override val data: DataLibrus,
                                   val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiTeacherFreeDayTypes"
    }

    init {
        apiGet(TAG, "TeacherFreeDays/Types") { json ->
            val teacherAbsenceTypes = json.getJsonArray("Types").asJsonObjectList()

            teacherAbsenceTypes?.forEach { teacherAbsenceType ->
                val id = teacherAbsenceType.getLong("Id") ?: return@forEach
                val name = teacherAbsenceType.getString("Name") ?: return@forEach

                val teacherAbsenceTypeObject = TeacherAbsenceType(
                        profileId,
                        id,
                        name
                )

                data.teacherAbsenceTypes.put(id, teacherAbsenceTypeObject)
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_TEACHER_FREE_DAY_TYPES, 7 * DAY)
            onSuccess()
        }
    }
}

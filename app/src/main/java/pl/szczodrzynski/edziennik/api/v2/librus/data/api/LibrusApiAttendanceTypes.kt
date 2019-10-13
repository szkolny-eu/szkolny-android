/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-13
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.ENDPOINT_LIBRUS_API_ATTENDANCE_TYPES
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS

class LibrusApiAttendanceTypes(override val data: DataLibrus,
                               val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiAttendanceTypes"
    }

    init {
        apiGet(TAG, "Attendances/Types") { json ->
            val attendanceTypes = json.getJsonArray("Types")

            attendanceTypes?.forEach { attendanceTypeEl ->
                val attendanceType = attendanceTypeEl.asJsonObject

                val id = attendanceType.getInt("Id") ?: return@forEach
                val standardId = when (attendanceType.getBoolean("Standard") ?: false) {
                    true -> id
                    false -> attendanceType.getJsonObject("StandardType")?.getInt("Id")
                            ?: return@forEach
                }

                val name = attendanceType.getString("Name") ?: ""

                data.attendanceTypes.put(id, Pair(standardId, name))
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_ATTENDANCE_TYPES, SYNC_ALWAYS)
            onSuccess()
        }
    }
}

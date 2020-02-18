/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-13
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import android.graphics.Color
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_ATTENDANCE_TYPES
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.data.db.entity.AttendanceType

class LibrusApiAttendanceTypes(override val data: DataLibrus,
                               override val lastSync: Long?,
                               val onSuccess: (endpointId: Int) -> Unit
) : LibrusApi(data, lastSync) {
    companion object {
        const val TAG = "LibrusApiAttendanceTypes"
    }

    init {
        apiGet(TAG, "Attendances/Types") { json ->
            val attendanceTypes = json.getJsonArray("Types")?.asJsonObjectList()

            attendanceTypes?.forEach { attendanceType ->
                val id = attendanceType.getLong("Id") ?: return@forEach
                val name = attendanceType.getString("Name") ?: ""
                val color = attendanceType.getString("ColorRGB")?.let { Color.parseColor("#$it") } ?: -1

                val standardId = when (attendanceType.getBoolean("Standard") ?: false) {
                    true -> id
                    false -> attendanceType.getJsonObject("StandardType")?.getLong("Id") ?: id
                }
                val type = when (standardId) {
                    1L -> Attendance.TYPE_ABSENT
                    2L -> Attendance.TYPE_BELATED
                    3L -> Attendance.TYPE_ABSENT_EXCUSED
                    4L -> Attendance.TYPE_RELEASED
                    /*100*/else -> Attendance.TYPE_PRESENT
                }

                data.attendanceTypes.put(id, AttendanceType(profileId, id, name, type, color))
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_ATTENDANCE_TYPES, 4*DAY)
            onSuccess(ENDPOINT_LIBRUS_API_ATTENDANCE_TYPES)
        }
    }
}

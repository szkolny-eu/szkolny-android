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

                val typeName = attendanceType.getString("Name") ?: ""
                val typeSymbol = attendanceType.getString("Short") ?: ""
                val typeColor = attendanceType.getString("ColorRGB")?.let { Color.parseColor("#$it") }

                val isStandard = attendanceType.getBoolean("Standard") ?: false
                val baseType = when (attendanceType.getJsonObject("StandardType")?.getLong("Id") ?: id) {
                    1L -> Attendance.TYPE_ABSENT
                    2L -> Attendance.TYPE_BELATED
                    3L -> Attendance.TYPE_ABSENT_EXCUSED
                    4L -> Attendance.TYPE_RELEASED
                    /*100*/else -> when (isStandard) {
                        true -> Attendance.TYPE_PRESENT
                        false -> Attendance.TYPE_PRESENT_CUSTOM
                    }
                }
                val typeShort = when (isStandard) {
                    true -> data.app.attendanceManager.getTypeShort(baseType)
                    false -> typeSymbol
                }

                data.attendanceTypes.put(id, AttendanceType(
                        profileId,
                        id,
                        baseType,
                        typeName,
                        typeShort,
                        typeSymbol,
                        typeColor
                ))
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_ATTENDANCE_TYPES, 2*DAY)
            onSuccess(ENDPOINT_LIBRUS_API_ATTENDANCE_TYPES)
        }
    }
}

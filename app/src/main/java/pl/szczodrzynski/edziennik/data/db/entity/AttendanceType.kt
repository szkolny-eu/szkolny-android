/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-23. 
 */

package pl.szczodrzynski.edziennik.data.db.entity

import androidx.room.Entity
import androidx.room.Ignore

@Entity(tableName = "attendanceTypes",
        primaryKeys = ["profileId", "id"])
data class AttendanceType (
        val profileId: Int,
        val id: Long,
        /** Base type ID used to count attendance stats */
        val baseType: Int,
        /** A full type name coming from the e-register */
        val typeName: String,
        /** A short name to display by default, might be different for non-standard types */
        val typeShort: String,
        /** A short name that the e-register would display */
        val typeSymbol: String,
        /** A color that the e-register would display, null falls back to app's default */
        val typeColor: Int?
) : Comparable<AttendanceType> {

        @Ignore
        var isCounted: Boolean = true

        // attendance bar order:
        // day_free, present, present_custom, unknown, belated_excused, belated, released, absent_excused, absent,
        override fun compareTo(other: AttendanceType): Int {
                val type1 = when (baseType) {
                        Attendance.TYPE_DAY_FREE -> 0
                        Attendance.TYPE_PRESENT -> 1
                        Attendance.TYPE_PRESENT_CUSTOM -> 2
                        Attendance.TYPE_UNKNOWN -> 3
                        Attendance.TYPE_BELATED_EXCUSED -> 4
                        Attendance.TYPE_BELATED -> 5
                        Attendance.TYPE_RELEASED -> 6
                        Attendance.TYPE_ABSENT_EXCUSED -> 7
                        Attendance.TYPE_ABSENT -> 8
                        else -> 9
                }
                val type2 = when (other.baseType) {
                        Attendance.TYPE_DAY_FREE -> 0
                        Attendance.TYPE_PRESENT -> 1
                        Attendance.TYPE_PRESENT_CUSTOM -> 2
                        Attendance.TYPE_UNKNOWN -> 3
                        Attendance.TYPE_BELATED_EXCUSED -> 4
                        Attendance.TYPE_BELATED -> 5
                        Attendance.TYPE_RELEASED -> 6
                        Attendance.TYPE_ABSENT_EXCUSED -> 7
                        Attendance.TYPE_ABSENT -> 8
                        else -> 9
                }
                return type1 - type2
        }
}

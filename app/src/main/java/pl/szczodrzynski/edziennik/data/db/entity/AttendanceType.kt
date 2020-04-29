/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-23. 
 */

package pl.szczodrzynski.edziennik.data.db.entity

import androidx.room.Entity

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
)

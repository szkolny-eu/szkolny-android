/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-23. 
 */

package pl.szczodrzynski.edziennik.data.db.modules.attendance

import androidx.room.Entity

@Entity(tableName = "attendanceTypes",
        primaryKeys = ["profileId", "id"])
data class AttendanceType (

        val profileId: Int,

        val id: Long,

        val name: String,

        val type: Int,

        val color: Int

)

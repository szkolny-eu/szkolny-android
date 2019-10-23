/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-23.
 */

package pl.szczodrzynski.edziennik.data.db.modules.classrooms

import androidx.room.Entity

@Entity(tableName = "classrooms",
        primaryKeys = ["profileId", "id"])
data class Classroom (

        val profileId: Int,

        val id: Long,

        val name: String

)

/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-23. 
 */

package pl.szczodrzynski.edziennik.data.db.modules.notices

import androidx.room.Entity

@Entity(tableName = "noticeTypes",
        primaryKeys = ["profileId", "id"])
data class NoticeType (

        val profileId: Int,

        val id: Long,

        val name: String

)

/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-24.
 */
package pl.szczodrzynski.edziennik.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import pl.szczodrzynski.edziennik.utils.models.Date

@Entity(tableName = "announcements",
        primaryKeys = ["profileId", "announcementId"],
        indices = [
            Index(value = ["profileId"])
        ])
open class Announcement(
        val profileId: Int,
        @ColumnInfo(name = "announcementId")
        var id: Long,
        @ColumnInfo(name = "announcementSubject")
        var subject: String,
        @ColumnInfo(name = "announcementText")
        var text: String?,

        @ColumnInfo(name = "announcementStartDate")
        var startDate: Date?,
        @ColumnInfo(name = "announcementEndDate")
        var endDate: Date?,

        var teacherId: Long,
        var addedDate: Long = System.currentTimeMillis()
) : Keepable() {

    @ColumnInfo(name = "announcementIdString")
    var idString: String? = null

    @Ignore
    var showAsUnseen: Boolean? = null
}

/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-25.
 */
package pl.szczodrzynski.edziennik.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index

@Entity(tableName = "notices",
        primaryKeys = ["profileId", "noticeId"],
        indices = [
            Index(value = ["profileId"])
        ])
open class Notice(
        val profileId: Int,
        @ColumnInfo(name = "noticeId")
        var id: Long,
        @ColumnInfo(name = "noticeType")
        var type: Int,
        @ColumnInfo(name = "noticeSemester")
        var semester: Int,

        @ColumnInfo(name = "noticeText")
        var text: String,
        @ColumnInfo(name = "noticeCategory")
        var category: String?,
        @ColumnInfo(name = "noticePoints")
        var points: Float?,

        var teacherId: Long,
        var addedDate: Long = System.currentTimeMillis()
) : Keepable(), Noteable {
    companion object {
        const val TYPE_NEUTRAL = 0
        const val TYPE_POSITIVE = 1
        const val TYPE_NEGATIVE = 2
    }

    @Ignore
    var showAsUnseen = false

    override fun getNoteType() = Note.OwnerType.BEHAVIOR
}

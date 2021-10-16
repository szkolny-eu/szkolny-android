/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-16.
 */

package pl.szczodrzynski.edziennik.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["profileId", "noteOwnerType", "noteOwnerId"]),
    ],
)
data class Note(
    val profileId: Int,

    @PrimaryKey
    @ColumnInfo(name = "noteId")
    val id: Long,

    @ColumnInfo(name = "noteOwnerType")
    val ownerType: OwnerType?,
    @ColumnInfo(name = "noteOwnerId")
    val ownerId: Long?,
    @ColumnInfo(name = "noteReplacesOriginal")
    val replacesOriginal: Boolean = false,

    @ColumnInfo(name = "noteTopic")
    val topic: String?,
    @ColumnInfo(name = "noteBody")
    val body: String,
    @ColumnInfo(name = "noteColor")
    val color: Int?,

    @ColumnInfo(name = "noteSharedBy")
    val sharedBy: String? = null,
    @ColumnInfo(name = "noteSharedByName")
    val sharedByName: String? = null,

    val addedDate: Long = System.currentTimeMillis(),
) {
    enum class OwnerType(
        val isShareable: Boolean,
        val canReplace: Boolean,
    ) {
        ATTENDANCE(isShareable = false, canReplace = true),
        BEHAVIOR(isShareable = false, canReplace = false),
        DAY(isShareable = true, canReplace = false),
        EVENT(isShareable = true, canReplace = true),
        EVENT_SUBJECT(isShareable = true, canReplace = true),
        GRADE(isShareable = false, canReplace = true),
        LESSON(isShareable = true, canReplace = false),
        LESSON_SUBJECT(isShareable = true, canReplace = false),
        MESSAGE(isShareable = true, canReplace = false),
    }
}

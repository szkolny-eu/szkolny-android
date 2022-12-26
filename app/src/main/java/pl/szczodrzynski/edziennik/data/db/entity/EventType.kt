/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-19.
 */
package pl.szczodrzynski.edziennik.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "eventTypes",
    primaryKeys = ["profileId", "eventType"]
)
class EventType(
    val profileId: Int,

    @ColumnInfo(name = "eventType")
    val id: Long,

    @ColumnInfo(name = "eventTypeName")
    val name: String,
    @ColumnInfo(name = "eventTypeColor")
    val color: Int,
    @ColumnInfo(name = "eventTypeOrder")
    var order: Int = id.toInt(),
    @ColumnInfo(name = "eventTypeSource")
    val source: Int = SOURCE_REGISTER
) {
    companion object {
        const val SOURCE_DEFAULT = 0
        const val SOURCE_REGISTER = 1
        const val SOURCE_CUSTOM = 2
        const val SOURCE_SHARED = 3
    }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2024-7-1.
 */

package pl.szczodrzynski.edziennik.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(index = true)
    val timestamp: Long,
    val priority: Int,
    val tag: String?,
    val message: String,
)

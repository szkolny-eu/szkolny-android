/*
 * Copyright (c) Kuba Szczodrzyński 2024-7-1.
 */

package pl.szczodrzynski.edziennik.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pl.szczodrzynski.edziennik.data.db.entity.LogEntry

@Dao
interface LogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun add(entry: LogEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addAll(list: List<LogEntry>)

    @Query("DELETE FROM logs")
    suspend fun clear()
}

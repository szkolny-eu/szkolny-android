/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-27.
 */

package pl.szczodrzynski.edziennik.config.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(entry: ConfigEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAll(list: List<ConfigEntry>)

    @Query("SELECT * FROM config")
    fun getAllNow(): List<ConfigEntry>

    @Query("SELECT * FROM config WHERE profileId = :profileId")
    fun getAllNow(profileId: Int): List<ConfigEntry>

    @Query("DELETE FROM config WHERE profileId = :profileId")
    fun clear(profileId: Int)
}

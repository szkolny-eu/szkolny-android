/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-26.
 */

package pl.szczodrzynski.edziennik.config

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

    @Query("SELECT * FROM config WHERE profileId = -1")
    fun getAllNow(): List<ConfigEntry>

    @Query("SELECT * FROM config WHERE profileId = :profileId")
    fun getAllNow(profileId: Int): List<ConfigEntry>
}
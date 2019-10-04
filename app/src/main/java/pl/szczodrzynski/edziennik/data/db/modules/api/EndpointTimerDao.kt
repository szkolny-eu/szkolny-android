/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-3.
 */

package pl.szczodrzynski.edziennik.data.db.modules.api

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EndpointTimerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(endpointTimer: EndpointTimer)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAll(endpointTimerList: List<EndpointTimer>)

    @Query("SELECT * FROM endpointTimers WHERE profileId = :profileId")
    fun getAllNow(profileId: Int): List<EndpointTimer>

    @Query("DELETE FROM endpointTimers WHERE profileId = :profileId")
    fun clear(profileId: Int)
}
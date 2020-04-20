/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pl.szczodrzynski.edziennik.data.db.entity.EndpointTimer

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

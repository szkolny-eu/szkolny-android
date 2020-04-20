/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-22.
 */

package pl.szczodrzynski.edziennik.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import pl.szczodrzynski.edziennik.data.db.entity.TimetableManual
import pl.szczodrzynski.edziennik.utils.models.Date

@Dao
interface TimetableManualDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(timetableManual: TimetableManual)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAll(timetableManualList: List<TimetableManual>)

    @Query("SELECT * FROM timetableManual WHERE profileId = :profileId")
    fun getAll(profileId: Int): LiveData<List<TimetableManual>>

    @Query("SELECT * FROM timetableManual WHERE profileId = :profileId AND date >= :dateFrom AND date <= :dateTo")
    fun getAllByDateRange(profileId: Int, dateFrom: Date, dateTo: Date): LiveData<List<TimetableManual>>

    @Query("SELECT * FROM timetableManual WHERE profileId = :profileId AND (date IS NULL OR date = 0 OR date >= :dateFrom)")
    fun getAllToDisplay(profileId: Int, dateFrom: Date): LiveData<List<TimetableManual>>

    @Delete
    fun delete(timetableManual: TimetableManual)

    @Query("DELETE FROM timetableManual WHERE profileId = :profileId")
    fun clear(profileId: Int)
}

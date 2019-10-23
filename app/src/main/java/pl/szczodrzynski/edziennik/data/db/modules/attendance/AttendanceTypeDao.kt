/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-23. 
 */

package pl.szczodrzynski.edziennik.data.db.modules.attendance

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AttendanceTypeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(attendanceType: AttendanceType)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAll(attendanceTypeList: List<AttendanceType>)

    @Query("DELETE FROM attendanceTypes WHERE profileId = :profileId")
    fun clear(profileId: Int)

    @Query("SELECT * FROM attendanceTypes WHERE profileId = :profileId ORDER BY id ASC")
    fun getAllNow(profileId: Int): List<AttendanceType>
}

/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-23.
 */

package pl.szczodrzynski.edziennik.data.db.modules.classrooms

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ClassroomDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(classroom: Classroom)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAll(classroomList: List<Classroom>)

    @Query("DELETE FROM classrooms WHERE profileId = :profileId")
    fun clear(profileId: Int)

    @Query("SELECT * FROM classrooms WHERE profileId = :profileId ORDER BY id ASC")
    fun getAllNow(profileId: Int): List<Classroom>
}

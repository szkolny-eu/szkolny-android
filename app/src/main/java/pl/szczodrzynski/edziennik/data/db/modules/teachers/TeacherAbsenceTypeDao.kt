/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-18
 */

package pl.szczodrzynski.edziennik.data.db.modules.teachers

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TeacherAbsenceTypeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(teacherAbsence: TeacherAbsenceType)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAll(teacherAbsenceList: List<TeacherAbsenceType>)

    @Query("DELETE FROM teacherAbsenceTypes WHERE profileId = :profileId")
    fun clear(profileId: Int)

    @Query("SELECT * FROM teacherAbsenceTypes WHERE profileId = :profileId")
    fun getAllNow(profileId: Int): List<TeacherAbsenceType>
}

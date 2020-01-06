/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.TeacherAbsence
import pl.szczodrzynski.edziennik.data.db.full.TeacherAbsenceFull
import pl.szczodrzynski.edziennik.utils.models.Date

@Dao
interface TeacherAbsenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(teacherAbsence: TeacherAbsence)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAll(teacherAbsenceList: List<TeacherAbsence>)

    @Query("SELECT * FROM teacherAbsence WHERE profileId = :profileId")
    fun getAll(profileId: Int): List<TeacherAbsence>

    @Query("SELECT *, teachers.teacherName || ' ' || teachers.teacherSurname as teacherFullName, " +
            "metadata.seen, metadata.notified, metadata.addedDate FROM teacherAbsence " +
            "LEFT JOIN teachers USING (profileId, teacherId) " +
            "LEFT JOIN metadata ON teacherAbsenceId = thingId AND metadata.thingType = " + Metadata.TYPE_TEACHER_ABSENCE +
            " AND metadata.profileId = :profileId WHERE teachers.profileId = :profileId")
    fun getAllFullNow(profileId: Int): List<TeacherAbsenceFull>

    @Query("SELECT *, teachers.teacherName || ' ' || teachers.teacherSurname as teacherFullName, " +
            "metadata.seen, metadata.notified, metadata.addedDate FROM teacherAbsence " +
            "LEFT JOIN teachers USING (profileId, teacherId) " +
            "LEFT JOIN metadata ON teacherAbsenceId = thingId AND metadata.thingType = " + Metadata.TYPE_TEACHER_ABSENCE +
            " AND metadata.profileId = :profileId WHERE teachers.profileId = :profileId " +
            "AND :date BETWEEN teacherAbsenceDateFrom AND teacherAbsenceDateTo")
    fun getAllByDateFull(profileId: Int, date: Date): LiveData<List<TeacherAbsenceFull>>

    @Query("DELETE FROM teacherAbsence WHERE profileId = :profileId")
    fun clear(profileId: Int)
}

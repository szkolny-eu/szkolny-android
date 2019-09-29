package pl.szczodrzynski.edziennik.data.db.modules.teachers

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
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
    fun getAllFull(profileId: Int): List<TeacherAbsenceFull>

    @Query("SELECT *, teachers.teacherName || ' ' || teachers.teacherSurname as teacherFullName, " +
            "metadata.seen, metadata.notified, metadata.addedDate FROM teacherAbsence " +
            "LEFT JOIN teachers USING (profileId, teacherId) " +
            "LEFT JOIN metadata ON teacherAbsenceId = thingId AND metadata.thingType = " + Metadata.TYPE_TEACHER_ABSENCE +
            " AND metadata.profileId = :profileId WHERE teachers.profileId = :profileId " +
            "AND :date BETWEEN teacherAbsenceDateFrom AND teacherAbsenceDateTo")
    fun getAllByDateFull(profileId: Int, date: Date): LiveData<List<TeacherAbsenceFull>>
}

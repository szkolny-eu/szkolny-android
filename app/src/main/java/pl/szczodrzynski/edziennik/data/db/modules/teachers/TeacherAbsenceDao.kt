package pl.szczodrzynski.edziennik.data.db.modules.teachers

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pl.szczodrzynski.edziennik.utils.models.Date

@Dao
interface TeacherAbsenceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(teacherAbsence: TeacherAbsence)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAll(teacherAbsenceList: List<TeacherAbsence>)

    @Query("SELECT profileId, teacherAbsenceDateFrom, teacherAbsenceDateTo, COUNT(*) as teacherAbsenceCount" +
            "from teacherAbsence WHERE profileId = :profileId GROUP BY teacherAbsenceDateFrom, teacherAbsenceDateTo")
    fun getCounters(profileId: Int)

    @Query("SELECT profileId, teacherAbsenceDateFrom, teacherAbsenceDateTo, COUNT(*) as teacherAbsenceCount" +
            "from teacherAbsence WHERE profileId = :profileId AND :date BETWEEN teacherAbsenceDateFrom and teacherAbsenceDateTo" +
            "GROUP BY teacherAbsenceDateFrom, teacherAbsenceDateTo")
    fun getCounterByDate(profileId: Int, date: Date)
}

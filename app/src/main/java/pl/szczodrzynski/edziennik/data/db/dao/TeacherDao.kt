package pl.szczodrzynski.edziennik.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteQuery
import pl.szczodrzynski.edziennik.data.db.entity.Teacher

@Dao
interface TeacherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(teacher: Teacher)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAll(teacherList: List<Teacher>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addAllIgnore(teacherList: List<Teacher>)

    @RawQuery
    fun query(query: SupportSQLiteQuery): Int

    @Query("DELETE FROM teachers WHERE profileId = :profileId")
    fun clear(profileId: Int)

    @Query("SELECT * FROM teachers WHERE profileId = :profileId AND teacherId = :id")
    fun getById(profileId: Int, id: Long): LiveData<Teacher?>

    @Query("SELECT * FROM teachers WHERE profileId = :profileId AND teacherId = :id")
    fun getByIdNow(profileId: Int, id: Long): Teacher?

    @Query("SELECT * FROM teachers WHERE profileId = :profileId AND teacherType <= 127 ORDER BY teacherName, teacherSurname ASC")
    fun getAllTeachers(profileId: Int): LiveData<List<Teacher>>

    @Query("SELECT * FROM teachers WHERE profileId = :profileId ORDER BY teacherName, teacherSurname ASC")
    fun getAllNow(profileId: Int): List<Teacher>

    @Query("UPDATE teachers SET teacherLoginId = :loginId WHERE profileId = :profileId AND teacherId = :id")
    fun updateLoginId(profileId: Int, id: Long, loginId: String?)
}

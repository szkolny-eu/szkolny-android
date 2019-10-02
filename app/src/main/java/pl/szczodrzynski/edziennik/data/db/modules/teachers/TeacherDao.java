package pl.szczodrzynski.edziennik.data.db.modules.teachers;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TeacherDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void add(Teacher teacher);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addAll(List<Teacher> teacherList);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void addAllIgnore(List<Teacher> teacherList);

    @Query("DELETE FROM teachers WHERE profileId = :profileId")
    void clear(int profileId);

    @Query("SELECT * FROM teachers WHERE profileId = :profileId AND teacherId = :id")
    LiveData<Teacher> getById(int profileId, long id);

    @Query("SELECT * FROM teachers WHERE profileId = :profileId AND teacherId = :id")
    Teacher getByIdNow(int profileId, long id);

    @Query("SELECT * FROM teachers WHERE profileId = :profileId AND teacherType <= 127 ORDER BY teacherName, teacherSurname ASC")
    LiveData<List<Teacher>> getAllTeachers(int profileId);

    @Query("SELECT * FROM teachers WHERE profileId = :profileId ORDER BY teacherName, teacherSurname ASC")
    List<Teacher> getAllNow(int profileId);

    @Query("UPDATE teachers SET teacherLoginId = :loginId WHERE profileId = :profileId AND teacherId = :id")
    void updateLoginId(int profileId, long id, String loginId);
}

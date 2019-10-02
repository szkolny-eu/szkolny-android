package pl.szczodrzynski.edziennik.data.db.modules.subjects;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SubjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void add(Subject subject);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addAll(List<Subject> subjectList);

    @Query("DELETE FROM subjects WHERE profileId = :profileId")
    void clear(int profileId);

    @Query("SELECT * FROM subjects WHERE profileId = :profileId AND subjectId = :id")
    LiveData<Subject> getById(int profileId, long id);

    @Query("SELECT * FROM subjects WHERE profileId = :profileId AND subjectId = :id")
    Subject getByIdNow(int profileId, long id);

    @Query("SELECT * FROM subjects WHERE profileId = :profileId ORDER BY subjectLongName ASC")
    LiveData<List<Subject>> getAll(int profileId);

    @Query("SELECT * FROM subjects WHERE profileId = :profileId ORDER BY subjectLongName ASC")
    List<Subject> getAllNow(int profileId);
}

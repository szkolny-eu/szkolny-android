package pl.szczodrzynski.edziennik.data.db.modules.grades;

import android.util.LongSparseArray;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.room.Transaction;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.List;

import static pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata.TYPE_GRADE;

@Dao
public abstract class GradeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract long add(Grade grade);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void addAll(List<Grade> gradeList);

    @Query("DELETE FROM grades WHERE profileId = :profileId")
    public abstract void clear(int profileId);

    @Query("DELETE FROM grades WHERE profileId = :profileId AND gradeType = :type")
    public abstract void clearWithType(int profileId, int type);

    @Query("DELETE FROM grades WHERE profileId = :profileId AND gradeSemester = :semester")
    public abstract void clearForSemester(int profileId, int semester);

    @Query("DELETE FROM grades WHERE profileId = :profileId AND gradeSemester = :semester AND gradeType = :type")
    public abstract void clearForSemesterWithType(int profileId, int semester, int type);

    @RawQuery(observedEntities = {Grade.class})
    abstract LiveData<List<GradeFull>> getAll(SupportSQLiteQuery query);
    public LiveData<List<GradeFull>> getAll(int profileId, String filter, String orderBy) {
        return getAll(new SimpleSQLiteQuery("SELECT \n" +
                "*, \n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS teacherFullName\n" +
                "FROM grades \n" +
                "LEFT JOIN subjects USING(profileId, subjectId)\n" +
                "LEFT JOIN teachers USING(profileId, teacherId)\n" +
                "LEFT JOIN metadata ON gradeId = thingId AND thingType = " + TYPE_GRADE + " AND metadata.profileId = "+profileId+"\n" +
                "WHERE grades.profileId = "+profileId+" AND "+filter+"\n" +
                "ORDER BY "+orderBy)); // TODO: 2019-04-30 why did I add sorting by gradeType???
    }
    public LiveData<List<GradeFull>> getAllOrderBy(int profileId, String orderBy) {
        return getAll(profileId, "1", orderBy);
    }
    public LiveData<List<GradeFull>> getAllWhere(int profileId, String filter) {
        return getAll(profileId, filter, "addedDate DESC");
    }

    @RawQuery
    abstract List<GradeFull> getAllNow(SupportSQLiteQuery query);
    public List<GradeFull> getAllNow(int profileId, String filter) {
        return getAllNow(new SimpleSQLiteQuery("SELECT \n" +
                "*, \n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS teacherFullName\n" +
                "FROM grades \n" +
                "LEFT JOIN subjects USING(profileId, subjectId)\n" +
                "LEFT JOIN teachers USING(profileId, teacherId)\n" +
                "LEFT JOIN metadata ON gradeId = thingId AND thingType = " + TYPE_GRADE + " AND metadata.profileId = "+profileId+"\n" +
                "WHERE grades.profileId = "+profileId+" AND "+filter+"\n" +
                "ORDER BY addedDate DESC"));
    }
    public List<GradeFull> getNotNotifiedNow(int profileId) {
        return getAllNow(profileId, "notified = 0");
    }
    public List<GradeFull> getAllWithParentIdNow(int profileId, long parentId) {
        return getAllNow(profileId, "gradeParentId = "+parentId);
    }

    @RawQuery
    abstract GradeFull getNow(SupportSQLiteQuery query);
    public GradeFull getNow(int profileId, String filter) {
        return getNow(new SimpleSQLiteQuery("SELECT \n" +
                "*, \n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS teacherFullName\n" +
                "FROM grades \n" +
                "LEFT JOIN subjects USING(profileId, subjectId)\n" +
                "LEFT JOIN teachers USING(profileId, teacherId)\n" +
                "LEFT JOIN metadata ON gradeId = thingId AND thingType = " + TYPE_GRADE + " AND metadata.profileId = "+profileId+"\n" +
                "WHERE grades.profileId = "+profileId+" AND "+filter+"\n" +
                "ORDER BY addedDate DESC"));
    }
    public GradeFull getByIdNow(int profileId, long gradeId) {
        return getNow(profileId, "gradeId = "+gradeId);
    }

    @Query("UPDATE grades SET gradeClassAverage = :classAverage, gradeColor = :color WHERE profileId = :profileId AND gradeId = :gradeId")
    public abstract void updateDetailsById(int profileId, long gradeId, float classAverage, int color);

    @Query("UPDATE metadata SET addedDate = :addedDate WHERE profileId = :profileId AND thingType = "+TYPE_GRADE+" AND thingId = :gradeId")
    public abstract void updateAddedDateById(int profileId, long gradeId, long addedDate);

    @Transaction
    public void updateDetails(int profileId, LongSparseArray<Float> gradeAverages, LongSparseArray<Long> gradeAddedDates, LongSparseArray<Integer> gradeColors) {
        for (int i = 0; i < gradeAverages.size(); i++) {
            long gradeId = gradeAverages.keyAt(i);
            float classAverage = gradeAverages.valueAt(i);
            long addedDate = gradeAddedDates.valueAt(i);
            int color = gradeColors.valueAt(i);
            updateDetailsById(profileId, gradeId, classAverage, color);
            updateAddedDateById(profileId, gradeId, addedDate);
        }
    }

    @Query("SELECT gradeId FROM grades WHERE profileId = :profileId ORDER BY gradeId")
    public abstract List<Integer> getIds(int profileId);
    @Query("SELECT gradeClassAverage FROM grades WHERE profileId = :profileId ORDER BY gradeId")
    public abstract List<Float> getClassAverages(int profileId);
    @Query("SELECT gradeColor FROM grades WHERE profileId = :profileId ORDER BY gradeId")
    public abstract List<Integer> getColors(int profileId);
    @Query("SELECT addedDate FROM metadata WHERE profileId = :profileId AND thingType = "+TYPE_GRADE+" ORDER BY thingId")
    public abstract List<Long> getAddedDates(int profileId);
    @Transaction
    public void getDetails(int profileId, LongSparseArray<Long> gradeAddedDates, LongSparseArray<Float> gradeAverages, LongSparseArray<Integer> gradeColors) {
        List<Integer> ids = getIds(profileId);
        List<Float> classAverages = getClassAverages(profileId);
        List<Integer> colors = getColors(profileId);
        List<Long> addedDates = getAddedDates(profileId);
        for (int index = 0; index < ids.size(); index++) {
            if (classAverages.size() > index) {
                gradeAverages.put(ids.get(index), classAverages.get(index));
            }
            if (colors.size() > index) {
                gradeColors.put(ids.get(index), colors.get(index));
            }
            if (addedDates.size() > index) {
                gradeAddedDates.put(ids.get(index), addedDates.get(index));
            }
        }
    }

    public LiveData<List<GradeFull>> getAllFromDate(int profileId, int semester, long date) {
        return getAllWhere(profileId, "gradeSemester = " + semester + "  AND addedDate > " + date);
    }
}

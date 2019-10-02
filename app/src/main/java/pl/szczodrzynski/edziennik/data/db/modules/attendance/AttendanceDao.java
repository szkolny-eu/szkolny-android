package pl.szczodrzynski.edziennik.data.db.modules.attendance;

import androidx.lifecycle.LiveData;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RawQuery;
import pl.szczodrzynski.edziennik.utils.models.Date;

import java.util.List;

import static pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata.TYPE_ATTENDANCE;

@Dao
public abstract class AttendanceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract long add(Attendance attendance);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void addAll(List<Attendance> attendanceList);

    @Query("DELETE FROM attendances WHERE profileId = :profileId")
    public abstract void clear(int profileId);

    @Query("DELETE FROM attendances WHERE profileId = :profileId AND attendanceLessonDate > :date")
    public abstract void clearAfterDate(int profileId, Date date);

    @RawQuery(observedEntities = {Attendance.class})
    abstract LiveData<List<AttendanceFull>> getAll(SupportSQLiteQuery query);
    public LiveData<List<AttendanceFull>> getAll(int profileId, String filter) {
        return getAll(new SimpleSQLiteQuery("SELECT \n" +
                "*, \n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS teacherFullName\n" +
                "FROM attendances \n" +
                "LEFT JOIN teachers USING(profileId, teacherId)\n" +
                "LEFT JOIN subjects USING(profileId, subjectId)\n" +
                "LEFT JOIN metadata ON attendanceId = thingId AND thingType = " + TYPE_ATTENDANCE + " AND metadata.profileId = "+profileId+"\n" +
                "WHERE attendances.profileId = "+profileId+" AND "+filter+"\n" +
                "ORDER BY attendanceLessonDate DESC, attendanceStartTime DESC"));
    }
    public LiveData<List<AttendanceFull>> getAll(int profileId) {
        return getAll(profileId, "1");
    }
    public LiveData<List<AttendanceFull>> getAllWhere(int profileId, String filter) {
        return getAll(profileId, filter);
    }

    @RawQuery
    abstract List<AttendanceFull> getAllNow(SupportSQLiteQuery query);
    public List<AttendanceFull> getAllNow(int profileId, String filter) {
        return getAllNow(new SimpleSQLiteQuery("SELECT \n" +
                "*, \n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS teacherFullName\n" +
                "FROM attendances \n" +
                "LEFT JOIN teachers USING(profileId, teacherId)\n" +
                "LEFT JOIN subjects USING(profileId, subjectId)\n" +
                "LEFT JOIN metadata ON attendanceId = thingId AND thingType = " + TYPE_ATTENDANCE + " AND metadata.profileId = "+profileId+"\n" +
                "WHERE attendances.profileId = "+profileId+" AND "+filter+"\n" +
                "ORDER BY attendanceLessonDate DESC, attendanceStartTime DESC"));
    }
    public List<AttendanceFull> getNotNotifiedNow(int profileId) {
        return getAllNow(profileId, "notified = 0");
    }

    // only absent and absent_excused count as absences
    // all the other types are counted as being present
    @Query("SELECT \n" +
            "CAST(SUM(CASE WHEN attendanceType != "+Attendance.TYPE_ABSENT+" AND attendanceType != "+Attendance.TYPE_ABSENT_EXCUSED+" THEN 1 ELSE 0 END) AS float)\n" +
            " / \n" +
            "CAST(count() AS float)*100 \n" +
            "FROM attendances \n" +
            "WHERE profileId = :profileId")
    public abstract LiveData<Float> getAttendancePercentage(int profileId);

    @Query("SELECT \n" +
            "CAST(SUM(CASE WHEN attendanceType != "+Attendance.TYPE_ABSENT+" AND attendanceType != "+Attendance.TYPE_ABSENT_EXCUSED+" THEN 1 ELSE 0 END) AS float)\n" +
            " / \n" +
            "CAST(count() AS float)*100 \n" +
            "FROM attendances \n" +
            "WHERE profileId = :profileId AND attendanceSemester = :semester")
    public abstract float getAttendancePercentageNow(int profileId, int semester);

    @Query("SELECT \n" +
            "CAST(SUM(CASE WHEN attendanceType != "+Attendance.TYPE_ABSENT+" AND attendanceType != "+Attendance.TYPE_ABSENT_EXCUSED+" THEN 1 ELSE 0 END) AS float)\n" +
            " / \n" +
            "CAST(count() AS float)*100 \n" +
            "FROM attendances \n" +
            "WHERE profileId = :profileId")
    public abstract float getAttendancePercentageNow(int profileId);
}


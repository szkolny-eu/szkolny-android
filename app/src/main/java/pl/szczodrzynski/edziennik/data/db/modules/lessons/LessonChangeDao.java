package pl.szczodrzynski.edziennik.data.db.modules.lessons;

import androidx.lifecycle.LiveData;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RawQuery;

import java.util.List;

import pl.szczodrzynski.edziennik.utils.models.Date;
import pl.szczodrzynski.edziennik.ui.modules.agenda.lessonchange.LessonChangeCounter;

import static pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata.TYPE_LESSON_CHANGE;
import static pl.szczodrzynski.edziennik.utils.Utils.d;

@Dao
public abstract class LessonChangeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract long add(LessonChange lessonChange);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void addAll(List<LessonChange> lessonChangeList);

    @Query("DELETE FROM lessonChanges WHERE profileId = :profileId")
    public abstract void clear(int profileId);

    public static String getQueryString(int profileId, String filter) {
        return "SELECT\n" +
                "lessonChanges.profileId AS lessonChangeProfileId,\n" +
                "lessonChanges.lessonChangeId,\n" +
                "lessonChanges.lessonChangeDate,\n" +
                "lessonChanges.lessonChangeStartTime,\n" +
                "lessonChanges.lessonChangeType,\n" +
                "lessonChanges.lessonChangeClassroomName,\n" +
                "lessonChanges.subjectId AS changeSubjectId,\n" +
                "lessonChanges.teacherId AS changeTeacherId,\n" +
                "lessonChanges.teamId AS changeTeamId,\n" +
                "subjects.subjectLongName AS changeSubjectLongName,\n" +
                "subjects.subjectShortName AS changeSubjectShortName,\n" +
                "teams.teamName AS changeTeamName," +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS changeTeacherFullName,\n" +
                "metadata.seen, metadata.notified, metadata.addedDate\n" +
                "FROM lessonChanges\n" +
                "LEFT JOIN teachers USING(profileId, teacherId)\n" +
                "LEFT JOIN subjects USING(profileId, subjectId)\n" +
                "LEFT JOIN teams USING(profileId, teamId)\n" +
                "LEFT JOIN metadata ON lessonChangeId = thingId AND thingType = " + TYPE_LESSON_CHANGE + " AND metadata.profileId = lessonChanges.profileId\n" +// TODO validate this works!. I hope so
                "WHERE "+(profileId == -1 ? "" : "lessonChanges.profileId = "+profileId+" AND ")+filter+"\n" +
                "ORDER BY lessonChanges.profileId, lessonChangeDate, lessonChangeStartTime ASC";
    }

    @RawQuery(observedEntities = {Lesson.class, LessonChange.class})
    abstract LiveData<List<LessonFull>> getAll(SupportSQLiteQuery query);
    @RawQuery
    abstract List<LessonFull> getAllNow(SupportSQLiteQuery query);
    @RawQuery
    abstract LessonFull getNow(SupportSQLiteQuery query);

    public String getQueryWithLessons(int profileId, String filter) {
        return "SELECT\n" +
                "lessonChanges.profileId,\n" +
                "lessonChangeId,\n" +
                "lessonChangeDate AS lessonDate,\n" +
                "lessonChangeStartTime,\n" +
                "lessonChangeType,\n" +
                "lessonChangeClassroomName,\n" +
                "lessonChanges.subjectId AS changeSubjectId,\n" +
                "lessonChanges.teacherId AS changeTeacherId,\n" +
                "lessonChanges.teamId AS changeTeamId,\n" +
                "subjects.subjectLongName AS changeSubjectLongName,\n" +
                "subjects.subjectShortName AS changeSubjectShortName,\n" +
                "lessonsFull.*,\n" +
                "teams.teamName AS changeTeamName,teachers.teacherName || ' ' || teachers.teacherSurname AS changeTeacherFullName\n" +
                ",metadata.*FROM lessonChanges\n" +
                "LEFT JOIN teachers USING(profileId, teacherId)\n" +
                "LEFT JOIN subjects USING(profileId, subjectId)\n" +
                "LEFT JOIN teams USING(profileId, teamId)\n" +
                "LEFT JOIN metadata ON lessonChangeId = thingId AND thingType = 6 AND metadata.profileId = "+profileId+"\n" +
                "JOIN (\n" +
                "SELECT subjects.subjectLongName,\n" +
                "lessons.*,\n" +
                "subjects.subjectShortName,\n" +
                "teams.teamName,\n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS teacherFullName\n" +
                "FROM lessons\n" +
                "LEFT JOIN teachers USING(profileId, teacherId)\n" +
                "LEFT JOIN subjects USING(profileId, subjectId)\n" +
                "LEFT JOIN teams USING(profileId, teamId)\n" +
                "WHERE lessons.profileId = "+profileId+") lessonsFull ON lessonChangeWeekDay = lessonsFull.lessonWeekDay AND lessonChangeStartTime = lessonStartTime\n" +
                "WHERE lessonChanges.profileId = "+profileId+" AND "+filter+"\n" +
                "ORDER BY lessonChangeDate, lessonChangeStartTime ASC";
    }

    public List<LessonFull> getAllChangesWithLessonsNow(int profileId) {
        String query = getQueryWithLessons(profileId, "1");
        d("DB", query);
        return getAllNow(new SimpleSQLiteQuery(query));
    }

    public List<LessonFull> getNotNotifiedNow(int profileId) {
        return getAllNow(new SimpleSQLiteQuery(getQueryWithLessons(profileId, "notified = 0")));
    }

    @Query("SELECT profileId, lessonChangeDate, count(*) AS lessonChangeCount FROM lessonChanges WHERE profileId = :profileId GROUP BY lessonChangeDate")
    public abstract List<LessonChangeCounter> getLessonChangeCountersNow(int profileId);

    @Query("SELECT profileId, lessonChangeDate, count(*) AS lessonChangeCount FROM lessonChanges WHERE profileId = :profileId AND lessonChangeDate = :date GROUP BY lessonChangeDate")
    public abstract LiveData<LessonChangeCounter> getLessonChangeCounterByDate(int profileId, Date date);
}

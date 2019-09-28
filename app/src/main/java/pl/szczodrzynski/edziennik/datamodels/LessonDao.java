package pl.szczodrzynski.edziennik.datamodels;

import androidx.lifecycle.LiveData;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.annotation.NonNull;

import java.util.List;

import pl.szczodrzynski.edziennik.utils.models.Date;
import pl.szczodrzynski.edziennik.utils.models.Time;

@Dao
public abstract class LessonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract long add(Lesson lesson);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void addAll(List<Lesson> lessonList);

    @Query("DELETE FROM lessons WHERE profileId = :profileId")
    public abstract void clear(int profileId);

    @RawQuery(observedEntities = {Lesson.class, LessonChange.class})
    abstract LiveData<List<LessonFull>> getAll(SupportSQLiteQuery query);
    @RawQuery
    abstract List<LessonFull> getAllNow(SupportSQLiteQuery query);
    @RawQuery
    abstract LessonFull getNow(SupportSQLiteQuery query);

    public LiveData<List<LessonFull>> getAllByDate(int profileId, @NonNull Date date, @NonNull Time nowTime) {
        int weekDay = date.getWeekDay();
        String query = "SELECT\n" +
                "lessons.*,\n" +
                "subjects.subjectLongName,\n" +
                "subjects.subjectShortName,\n" +
                "teams.teamName,\n" +
                "lessonChangesFull.*,\n" +
                "('"+nowTime.getStringValue()+"' > lessonEndTime AND lessonWeekDay = "+weekDay+") AS lessonPassed,\n" +
                "('"+nowTime.getStringValue()+"' BETWEEN lessonStartTime AND lessonEndTime AND lessonWeekDay = "+weekDay+") AS lessonCurrent,\n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS teacherFullName\n" +
                "FROM lessons\n" +
                "LEFT JOIN teachers USING(profileId, teacherId)\n" +
                "LEFT JOIN subjects USING(profileId, subjectId)\n" +
                "LEFT JOIN teams USING(profileId, teamId)\n" +
                "LEFT JOIN \n" +
                "("
                +LessonChangeDao.getQueryString(profileId, "lessonChangeDate = '"+date.getStringY_m_d()+"'")+
                ") lessonChangesFull ON lessons.profileId = lessonChangesFull.lessonChangeProfileId AND lessonStartTime = lessonChangeStartTime\n" +
                "WHERE lessons.profileId = "+profileId+" AND lessonWeekDay = "+weekDay+"\n" +
                "ORDER BY lessonStartTime ASC";
        //Log.d("DB", "Query "+query);
        return getAll(new SimpleSQLiteQuery(query));
    }

    public List<LessonFull> getAllWeekNow(int profileId, @NonNull Date weekBeginDate, @NonNull Date todayDate) {
        String query = "SELECT\n" +
                "lessons.*,\n" +
                "subjects.subjectLongName,\n" +
                "subjects.subjectShortName,\n" +
                "teams.teamName,\n" +
                "lessonChangesFull.*,\n" +
                "date(\n" +
                "'"+weekBeginDate.getStringY_m_d()+"', \n" +
                "'+'||(case 1 when lessonWeekDay < "+todayDate.getWeekDay()+" then lessonWeekDay+7 else lessonWeekDay end)||' days'\n" +
                ") AS lessonDate,\n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS teacherFullName\n" +
                "FROM lessons\n" +
                "LEFT JOIN teachers USING(profileId, teacherId)\n" +
                "LEFT JOIN subjects USING(profileId, subjectId)\n" +
                "LEFT JOIN teams USING(profileId, teamId)\n" +
                "LEFT JOIN \n" +
                "("
                +LessonChangeDao.getQueryString(profileId, "1")+
                ") lessonChangesFull ON lessons.profileId = lessonChangesFull.lessonChangeProfileId AND lessonStartTime = lessonChangeStartTime AND lessonChangesFull.lessonChangeDate = lessonDate\n" +
                (profileId == -1 ? "" : "WHERE lessons.profileId = "+profileId+"\n") +
                "ORDER BY lessons.profileId, lessonDate, lessonStartTime ASC\n";
        //Log.d("DB", "Query "+query);
        return getAllNow(new SimpleSQLiteQuery(query));
    }

    public LessonFull getByDateTimeNow(int profileId, @NonNull Date date, @NonNull Time time) {
        String query = "SELECT\n" +
                "lessons.*,\n" +
                "subjects.subjectLongName,\n" +
                "subjects.subjectShortName,\n" +
                "teams.teamName,\n" +
                "lessonChangesFull.*,\n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS teacherFullName\n" +
                "FROM lessons\n" +
                "LEFT JOIN teachers USING(profileId, teacherId)\n" +
                "LEFT JOIN subjects USING(profileId, subjectId)\n" +
                "LEFT JOIN teams USING(profileId, teamId)\n" +
                "LEFT JOIN \n" +
                "("
                +LessonChangeDao.getQueryString(profileId, "lessonChangeDate = '"+date.getStringY_m_d()+"'")+
                ") lessonChangesFull ON lessons.profileId = lessonChangesFull.lessonChangeProfileId AND lessonStartTime = lessonChangeStartTime\n" +
                "WHERE lessons.profileId = "+profileId+" AND lessonWeekDay = "+date.getWeekDay()+" AND ('"+time.getStringValue()+"' BETWEEN lessonStartTime AND lessonEndTime)\n" +
                "ORDER BY lessonStartTime ASC";
        //Log.d("DB", "Query "+query);
        return getNow(new SimpleSQLiteQuery(query));
    }

    public List<LessonFull> getAllNearestNow(int profileId, @NonNull Date weekBeginDate, @NonNull Date todayDate, @NonNull Time nowTime) {
        int todayWeekDay = todayDate.getWeekDay();
        String query = "SELECT\n" +
                "lessons.*,\n" +
                "subjects.subjectLongName,\n" +
                "subjects.subjectShortName,\n" +
                "teams.teamName,\n" +
                "lessonChangesFull.*,\n" +
                "date(\n" +
                "'"+weekBeginDate.getStringY_m_d()+"', \n" +
                "'+'||(case 1 when lessonWeekDay < "+todayWeekDay+" then lessonWeekDay+7 else lessonWeekDay end)||' days'\n" +
                ") AS lessonDate,\n" +
                "('"+nowTime.getStringValue()+"' > lessonEndTime AND lessonWeekDay = "+todayWeekDay+") AS lessonPassed,\n" +
                "('"+nowTime.getStringValue()+"' BETWEEN lessonStartTime AND lessonEndTime AND lessonWeekDay = "+todayWeekDay+") AS lessonCurrent,\n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS teacherFullName\n" +
                "FROM lessons\n" +
                "LEFT JOIN teachers USING(profileId, teacherId)\n" +
                "LEFT JOIN subjects USING(profileId, subjectId)\n" +
                "LEFT JOIN teams USING(profileId, teamId)\n" +
                "LEFT JOIN \n" +
                "("
                +LessonChangeDao.getQueryString(profileId, "1")+
                ") lessonChangesFull ON lessons.profileId = lessonChangesFull.lessonChangeProfileId AND lessonStartTime = lessonChangeStartTime AND lessonChangesFull.lessonChangeDate = lessonDate\n" +
                "WHERE lessons.profileId = "+profileId+" AND (lessonWeekDay != "+todayWeekDay+" OR '"+nowTime.getStringValue()+"' < lessonEndTime OR '"+nowTime.getStringValue()+"' > lessonStartTime)\n" +
                "ORDER BY lessonDate, lessonStartTime ASC";
        //Log.d("DB", "Query "+query);
        return getAllNow(new SimpleSQLiteQuery(query));
    }

    public LiveData<List<LessonFull>> getAllByDateWithoutChanges(int profileId, @NonNull Date date) {
        String query = "SELECT\n" +
                "lessons.*,\n" +
                "subjects.subjectLongName,\n" +
                "subjects.subjectShortName,\n" +
                "teams.teamName,\n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS teacherFullName\n" +
                "FROM lessons\n" +
                "LEFT JOIN teachers USING(profileId, teacherId)\n" +
                "LEFT JOIN subjects USING(profileId, subjectId)\n" +
                "LEFT JOIN teams USING(profileId, teamId)\n" +
                "WHERE lessons.profileId = "+profileId+" AND lessonWeekDay = "+date.getWeekDay()+"\n" +
                "ORDER BY lessonStartTime ASC";
        //Log.d("DB", "Query "+query);
        return getAll(new SimpleSQLiteQuery(query));
    }

    public LessonFull getByDateTimeWithoutChangesNow(int profileId, @NonNull Date date, @NonNull Time time) {
        String query = "SELECT\n" +
                "lessons.*,\n" +
                "subjects.subjectLongName,\n" +
                "subjects.subjectShortName,\n" +
                "teams.teamName,\n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS teacherFullName\n" +
                "FROM lessons\n" +
                "LEFT JOIN teachers USING(profileId, teacherId)\n" +
                "LEFT JOIN subjects USING(profileId, subjectId)\n" +
                "LEFT JOIN teams USING(profileId, teamId)\n" +
                "WHERE lessons.profileId = "+profileId+" AND lessonWeekDay = "+date.getWeekDay()+" AND lessonStartTime = '"+time.getStringValue()+"'\n" +
                "ORDER BY lessonStartTime ASC";
        //Log.d("DB", "Query "+query);
        return getNow(new SimpleSQLiteQuery(query));
    }
}

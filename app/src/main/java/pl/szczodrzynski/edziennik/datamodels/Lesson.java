package pl.szczodrzynski.edziennik.datamodels;

import java.util.List;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.annotation.NonNull;

import pl.szczodrzynski.edziennik.utils.models.Time;
import pl.szczodrzynski.edziennik.utils.models.Week;

@Entity(tableName = "lessons",
        primaryKeys = {"profileId", "lessonWeekDay", "lessonStartTime", "lessonEndTime"},
        indices = {@Index(value = {"profileId", "lessonWeekDay"})})
public class Lesson {
    public int profileId;

    @ColumnInfo(name = "lessonWeekDay")
    public int weekDay;
    @NonNull
    @ColumnInfo(name = "lessonStartTime")
    public Time startTime;
    @NonNull
    @ColumnInfo(name = "lessonEndTime")
    public Time endTime;
    @ColumnInfo(name = "lessonClassroomName")
    public String classroomName;

    public long teacherId;
    public long subjectId;
    public long teamId;

    @Ignore
    public Lesson() {
        this.profileId = -1;
        this.startTime = new Time();
        this.endTime = new Time();
    }

    public Lesson(int profileId, int weekDay, @NonNull Time startTime, @NonNull Time endTime) {
        this.profileId = profileId;
        this.weekDay = weekDay;
        this.startTime = startTime;
        this.endTime = endTime;
        this.teacherId = -1;
        this.subjectId = -1;
        this.teamId = -1;
    }

    public Lesson(int profileId, int weekDay, String startTime, String endTime) {
        this(profileId, weekDay, new Time().parseFromYmdHm(startTime), new Time().parseFromYmdHm(endTime));
    }

    public Lesson(int profileId, String dateStr, String startTime, String endTime) {
        this(profileId, Week.getWeekDayFromDate(dateStr), startTime, endTime);
    }

    public static Lesson fromLessonChange(LessonChange lessonChange)
    {
        Lesson lesson = new Lesson(lessonChange.profileId, lessonChange.lessonDate.getWeekDay(), lessonChange.startTime, lessonChange.endTime);
        lesson.profileId = lessonChange.profileId;
        lesson.teacherId = lessonChange.teacherId;
        lesson.teamId = lessonChange.teamId;
        lesson.subjectId = lessonChange.subjectId;
        lesson.classroomName = lessonChange.classroomName;
        return lesson;
    }

    public static Lesson getByWeekDayAndSubject(List<Lesson> lessonList, int weekDay, long subjectId) {
        for (Lesson lesson: lessonList) {
            if (lesson.weekDay == weekDay && lesson.subjectId == subjectId)
                return lesson;
        }
        return null;
    }
}


package pl.szczodrzynski.edziennik.datamodels;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.annotation.NonNull;

import java.util.List;

import pl.szczodrzynski.edziennik.utils.models.Date;
import pl.szczodrzynski.edziennik.utils.models.Time;

@Entity(tableName = "lessonChanges",
        primaryKeys = {"profileId", "lessonChangeDate", "lessonChangeStartTime", "lessonChangeEndTime"},
        indices = {@Index(value = {"profileId", "lessonChangeDate"})})
public class LessonChange {
    public int profileId;

    @ColumnInfo(name = "lessonChangeId")
    public long id;

    @ColumnInfo(name = "lessonChangeWeekDay")
    public int weekDay;
    @NonNull
    @ColumnInfo(name = "lessonChangeDate")
    public Date lessonDate;
    @NonNull
    @ColumnInfo(name = "lessonChangeStartTime")
    public Time startTime;
    @NonNull
    @ColumnInfo(name = "lessonChangeEndTime")
    public Time endTime;
    @ColumnInfo(name = "lessonChangeClassroomName")
    public String classroomName;
    @ColumnInfo(name = "lessonChangeType")
    public int type;
    public static int TYPE_CANCELLED = 1;
    public static int TYPE_CHANGE = 2;
    public static int TYPE_ADDED = 3;

    public long teacherId;
    public long subjectId;
    public long teamId;

    public LessonChange()
    {
        this.profileId = -1;
        this.lessonDate = Date.getToday();
        this.weekDay = this.lessonDate.getWeekDay();
        this.startTime = Time.getNow();
        this.endTime = Time.getNow();
        this.id = System.currentTimeMillis();
    }

    public LessonChange(int profileId, String dateStr, String startTime, String endTime)
    {
        this(profileId, new Date().parseFromYmd(dateStr), new Time().parseFromYmdHm(startTime), new Time().parseFromYmdHm(endTime));
    }

    public LessonChange(int profileId, @NonNull Date date, @NonNull Time startTime, @NonNull Time endTime)
    {
        this.profileId = profileId;
        this.lessonDate = date;
        this.weekDay = this.lessonDate.getWeekDay();
        this.startTime = startTime;
        this.endTime = endTime;
        this.id = date.combineWith(startTime);
    }

    public Lesson getOriginalLesson(List<Lesson> lessonList)
    {
        int weekDay = this.lessonDate.getWeekDay();
        for (Lesson lesson: lessonList) {
            if (lesson.weekDay == weekDay
                    && lesson.startTime.getValue() == this.startTime.getValue()
                    && lesson.endTime.getValue() == this.endTime.getValue())
            {
                return lesson;
            }
        }
        return null;
    }

    public boolean matches(Lesson lesson) {
        if (lesson == null) {
            return false;
        }
        // we are assuming the start and end time is equal
        return this.profileId == lesson.profileId
                && this.teacherId == lesson.teacherId
                && this.subjectId == lesson.subjectId
                && this.teamId == lesson.teamId
                && this.classroomName.equals(lesson.classroomName);
    }
}



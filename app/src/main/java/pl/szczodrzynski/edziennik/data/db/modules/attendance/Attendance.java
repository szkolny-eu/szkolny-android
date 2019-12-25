package pl.szczodrzynski.edziennik.data.db.modules.attendance;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;

import pl.szczodrzynski.edziennik.utils.models.Date;
import pl.szczodrzynski.edziennik.utils.models.Time;

@Entity(tableName = "attendances",
        primaryKeys = {"profileId", "attendanceId", "attendanceLessonDate", "attendanceStartTime"},
        indices = {@Index(value = {"profileId"})})
public class Attendance {
    public int profileId;

    @ColumnInfo(name = "attendanceId")
    public long id;

    @NonNull
    @ColumnInfo(name = "attendanceLessonDate")
    public Date lessonDate;
    @NonNull
    @ColumnInfo(name = "attendanceStartTime")
    public Time startTime;
    @ColumnInfo(name = "attendanceLessonTopic")
    public String lessonTopic;
    @ColumnInfo(name = "attendanceSemester")
    public int semester;
    public static final int TYPE_PRESENT = 0;
    public static final int TYPE_ABSENT = 1;
    public static final int TYPE_ABSENT_EXCUSED = 2;
    public static final int TYPE_RELEASED = 3;
    public static final int TYPE_BELATED = 4;
    public static final int TYPE_BELATED_EXCUSED = 5;
    public static final int TYPE_CUSTOM = 6;
    public static final int TYPE_DAY_FREE = 7;
    @ColumnInfo(name = "attendanceType")
    public int type = TYPE_PRESENT;

    public long teacherId;
    public long subjectId;

    @Ignore
    public Attendance() {
        this(-1, -1, -1, -1, 0, "", Date.getToday(), Time.getNow(), TYPE_PRESENT);
    }

    public Attendance(int profileId, long id, long teacherId, long subjectId, int semester, String lessonTopic, Date lessonDate, Time startTime, int type) {
        this.profileId = profileId;
        this.id = id;
        this.teacherId = teacherId;
        this.subjectId = subjectId;
        this.semester = semester;
        this.lessonTopic = lessonTopic;
        this.lessonDate = lessonDate;
        this.startTime = startTime;
        this.type = type;
    }
}

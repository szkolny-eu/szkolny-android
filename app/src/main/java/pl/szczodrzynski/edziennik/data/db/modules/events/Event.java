package pl.szczodrzynski.edziennik.data.db.modules.events;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;

import pl.szczodrzynski.edziennik.utils.models.Date;
import pl.szczodrzynski.edziennik.utils.models.Time;

@Entity(tableName = "events",
        primaryKeys = {"profileId", "eventId"},
        indices = {@Index(value = {"profileId", "eventDate", "eventStartTime"}), @Index(value = {"profileId", "eventType"})})
public class Event {
    public int profileId;

    @ColumnInfo(name = "eventId")
    public long id;

    @ColumnInfo(name = "eventDate")
    public Date eventDate;
    @ColumnInfo(name = "eventStartTime")
    @Nullable
    public Time startTime; // null for allDay
    @ColumnInfo(name = "eventTopic")
    public String topic;
    @ColumnInfo(name = "eventColor")
    public int color = -1;
    public static final int TYPE_UNDEFINED = -2;
    public static final int TYPE_HOMEWORK = -1;
    public static final int TYPE_DEFAULT = 0;
    public static final int TYPE_EXAM = 1;
    public static final int TYPE_SHORT_QUIZ = 2;
    public static final int TYPE_ESSAY = 3;
    public static final int TYPE_PROJECT = 4;
    public static final int TYPE_PT_MEETING = 5;
    public static final int TYPE_EXCURSION = 6;
    public static final int TYPE_READING = 7;
    public static final int TYPE_CLASS_EVENT = 8;
    public static final int TYPE_INFORMATION = 9;
    public static final int TYPE_TEACHER_ABSENCE = 10;
    public static final int COLOR_HOMEWORK = 0xff795548;
    public static final int COLOR_DEFAULT = 0xffffc107;
    public static final int COLOR_EXAM = 0xfff44336;
    public static final int COLOR_SHORT_QUIZ = 0xff76ff03;
    public static final int COLOR_ESSAY = 0xFF4050B5;
    public static final int COLOR_PROJECT = 0xFF673AB7;
    public static final int COLOR_PT_MEETING = 0xff90caf9;
    public static final int COLOR_EXCURSION = 0xFF4CAF50;
    public static final int COLOR_READING = 0xFFFFEB3B;
    public static final int COLOR_CLASS_EVENT = 0xff388e3c;
    public static final int COLOR_INFORMATION = 0xff039be5;
    public static final int COLOR_TEACHER_ABSENCE = 0xff039be5;
    @ColumnInfo(name = "eventType")
    public int type = TYPE_DEFAULT;
    @ColumnInfo(name = "eventAddedManually")
    public boolean addedManually;
    @ColumnInfo(name = "eventSharedBy")
    public String sharedBy = null;
    @ColumnInfo(name = "eventSharedByName")
    public String sharedByName = null;
    @ColumnInfo(name = "eventBlacklisted")
    public boolean blacklisted = false;


    public long teacherId;
    public long subjectId;
    public long teamId;

    @Ignore
    public Event() {}

    public Event(int profileId, long id, Date eventDate, @Nullable Time startTime, String topic, int color, int type, boolean addedManually, long teacherId, long subjectId, long teamId)
    {
        this.profileId = profileId;
        this.id = id;
        this.eventDate = eventDate;
        this.startTime = startTime;
        this.topic = topic;
        this.color = color;
        this.type = type;
        this.addedManually = addedManually;
        this.teacherId = teacherId;
        this.subjectId = subjectId;
        this.teamId = teamId;
    }

    @Override
    public Event clone() throws CloneNotSupportedException {
        return new Event(
                profileId,
                id,
                eventDate.clone(),
                startTime == null ? null : startTime.clone(),
                topic,
                color,
                type,
                addedManually,
                subjectId,
                teacherId,
                teamId
        );
    }

    @Override
    public String toString() {
        return "Event{" +
                "profileId=" + profileId +
                ", id=" + id +
                ", eventDate=" + eventDate +
                ", startTime=" + startTime +
                ", topic='" + topic + '\'' +
                ", color=" + color +
                ", type=" + type +
                ", addedManually=" + addedManually +
                ", sharedBy='" + sharedBy + '\'' +
                ", sharedByName='" + sharedByName + '\'' +
                ", teacherId=" + teacherId +
                ", subjectId=" + subjectId +
                ", teamId=" + teamId +
                '}';
    }
}

package pl.szczodrzynski.edziennik.data.db.modules.metadata;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "metadata",
        indices = {@Index(value = {"profileId", "thingType", "thingId"}, unique = true)}
)
public class Metadata {
    public static final int TYPE_GRADE = 1;
    public static final int TYPE_NOTICE = 2;
    public static final int TYPE_ATTENDANCE = 3;
    public static final int TYPE_EVENT = 4;
    public static final int TYPE_HOMEWORK = 5;
    public static final int TYPE_LESSON_CHANGE = 6;
    public static final int TYPE_ANNOUNCEMENT = 7;
    public static final int TYPE_MESSAGE = 8;
    public static final int TYPE_TEACHER_ABSENCE = 9;
    public static final int TYPE_LUCKY_NUMBER = 10;

    public int profileId;

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "metadataId")
    public int id;

    public int thingType;
    public long thingId;

    public boolean seen;
    public boolean notified;
    public long addedDate;

    @Ignore
    public Metadata() {
        this.profileId = -1;
        this.seen = false;
        this.notified = false;
    }

    public Metadata(int profileId, int thingType, long thingId, boolean seen, boolean notified, long addedDate) {
        this.profileId = profileId;
        this.thingType = thingType;
        this.thingId = thingId;
        this.seen = seen;
        this.notified = notified;
        this.addedDate = addedDate;
    }

    public String thingType() {
        switch (thingType) {
            case TYPE_GRADE:
                return "TYPE_GRADE";
            case TYPE_NOTICE:
                return "TYPE_NOTICE";
            case TYPE_ATTENDANCE:
                return "TYPE_ATTENDANCE";
            case TYPE_EVENT:
                return "TYPE_EVENT";
            case TYPE_HOMEWORK:
                return "TYPE_HOMEWORK";
            case TYPE_LESSON_CHANGE:
                return "TYPE_LESSON_CHANGE";
            case TYPE_ANNOUNCEMENT:
                return "TYPE_ANNOUNCEMENT";
            case TYPE_MESSAGE:
                return "TYPE_MESSAGE";
            default:
                return "TYPE_UNKNOWN";
        }
    }

    @Override
    public String toString() {
        return "Metadata{" +
                "profileId=" + profileId +
                ", id=" + id +
                ", thingType=" + thingType() +
                ", thingId=" + thingId +
                ", seen=" + seen +
                ", notified=" + notified +
                ", addedDate=" + addedDate +
                '}';
    }
}

package pl.szczodrzynski.edziennik.datamodels;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import pl.szczodrzynski.edziennik.models.Date;

@Entity(tableName = "announcements",
        primaryKeys = {"profileId", "announcementId"},
        indices = {@Index(value = {"profileId"})})
public class Announcement {
    public int profileId;

    @ColumnInfo(name = "announcementId")
    public long id;

    @ColumnInfo(name = "announcementSubject")
    public String subject;
    @ColumnInfo(name = "announcementText")
    public String text;
    @Nullable
    @ColumnInfo(name = "announcementStartDate")
    public Date startDate;
    @Nullable
    @ColumnInfo(name = "announcementEndDate")
    public Date endDate;

    public long teacherId;

    @Ignore
    public Announcement() {}

    public Announcement(int profileId, long id, String subject, String text, @Nullable Date startDate, @Nullable Date endDate, long teacherId) {
        this.profileId = profileId;
        this.id = id;
        this.subject = subject;
        this.text = text;
        this.startDate = startDate;
        this.endDate = endDate;
        this.teacherId = teacherId;
    }
}




package pl.szczodrzynski.edziennik.data.db.modules.announcements;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;

import pl.szczodrzynski.edziennik.utils.models.Date;

@Entity(tableName = "announcements",
        primaryKeys = {"profileId", "announcementId"},
        indices = {@Index(value = {"profileId"})})
public class Announcement {
    public int profileId;

    @ColumnInfo(name = "announcementId")
    public long id;

    @ColumnInfo(name = "announcementSubject")
    public String subject;
    @Nullable
    @ColumnInfo(name = "announcementText")
    public String text;
    @Nullable
    @ColumnInfo(name = "announcementStartDate")
    public Date startDate;
    @Nullable
    @ColumnInfo(name = "announcementEndDate")
    public Date endDate;

    public long teacherId;

    @Nullable
    @ColumnInfo(name = "announcementIdString")
    public String idString;

    @Ignore
    public Announcement() {}

    public Announcement(int profileId, long id, String subject, String text, @Nullable Date startDate, @Nullable Date endDate, long teacherId, @Nullable String idString) {
        this.profileId = profileId;
        this.id = id;
        this.subject = subject;
        this.text = text;
        this.startDate = startDate;
        this.endDate = endDate;
        this.teacherId = teacherId;
        this.idString = idString;
    }
}




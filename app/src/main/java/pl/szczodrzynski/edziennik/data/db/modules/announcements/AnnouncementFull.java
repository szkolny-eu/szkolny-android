package pl.szczodrzynski.edziennik.data.db.modules.announcements;

public class AnnouncementFull extends Announcement {
    public String teacherFullName = "";

    // metadata
    public boolean seen;
    public boolean notified;
    public long addedDate;
}

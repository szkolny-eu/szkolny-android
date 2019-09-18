package pl.szczodrzynski.edziennik.datamodels;

public class EventFull extends Event {
    public String typeName = "";
    public int typeColor = -1;

    public String teacherFullName = "";

    public String subjectLongName = "";
    public String subjectShortName = "";

    public String teamName = "";

    // metadata
    public boolean seen;
    public boolean notified;
    public long addedDate;

    public int getColor() {
        return color == -1 ? typeColor : color;
    }

    @Override
    public String toString() {
        return "EventFull{" +
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
                ", blacklisted=" + blacklisted +
                ", teacherId=" + teacherId +
                ", subjectId=" + subjectId +
                ", teamId=" + teamId +
                ", typeName='" + typeName + '\'' +
                ", teacherFullName='" + teacherFullName + '\'' +
                ", subjectLongName='" + subjectLongName + '\'' +
                ", subjectShortName='" + subjectShortName + '\'' +
                ", teamName='" + teamName + '\'' +
                ", seen=" + seen +
                ", notified=" + notified +
                ", addedDate=" + addedDate +
                '}';
    }
}

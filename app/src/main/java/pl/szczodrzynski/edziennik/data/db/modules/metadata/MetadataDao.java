package pl.szczodrzynski.edziennik.data.db.modules.metadata;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

import pl.szczodrzynski.edziennik.data.db.modules.notices.Notice;
import pl.szczodrzynski.edziennik.data.db.modules.announcements.Announcement;
import pl.szczodrzynski.edziennik.data.db.modules.attendance.Attendance;
import pl.szczodrzynski.edziennik.data.db.modules.events.Event;
import pl.szczodrzynski.edziennik.data.db.modules.grades.Grade;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonChange;
import pl.szczodrzynski.edziennik.data.db.modules.lessons.LessonFull;
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message;
import pl.szczodrzynski.edziennik.utils.models.UnreadCounter;

import static pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata.TYPE_ANNOUNCEMENT;
import static pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata.TYPE_ATTENDANCE;
import static pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata.TYPE_EVENT;
import static pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata.TYPE_GRADE;
import static pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata.TYPE_HOMEWORK;
import static pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata.TYPE_LESSON_CHANGE;
import static pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata.TYPE_MESSAGE;
import static pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata.TYPE_NOTICE;

@Dao
public abstract class MetadataDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract long add(Metadata metadata);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void addAllIgnore(List<Metadata> metadataList);

    @Query("UPDATE metadata SET seen = :seen WHERE thingId = :thingId AND thingType = :thingType AND profileId = :profileId")
    abstract void updateSeen(int profileId, int thingType, long thingId, boolean seen);

    @Query("UPDATE metadata SET notified = :notified WHERE thingId = :thingId AND thingType = :thingType AND profileId = :profileId")
    abstract void updateNotified(int profileId, int thingType, long thingId, boolean notified);



    @Transaction
    public void setSeen(List<Metadata> metadataList) {
        for (Metadata metadata: metadataList) {
            if (add(metadata) == -1) {
                updateSeen(metadata.profileId, metadata.thingType, metadata.thingId, metadata.seen);
            }
        }
    }

    @Transaction
    public void setSeen(int profileId, Object o, boolean seen) {
        if (o instanceof Grade) {
            if (add(new Metadata(profileId, TYPE_GRADE, ((Grade) o).id, seen, false, 0)) == -1) {
                updateSeen(profileId, TYPE_GRADE, ((Grade) o).id, seen);
            }
        }
        if (o instanceof Attendance) {
            if (add(new Metadata(profileId, TYPE_ATTENDANCE, ((Attendance) o).id, seen, false, 0)) == -1) {
                updateSeen(profileId, TYPE_ATTENDANCE, ((Attendance) o).id, seen);
            }
        }
        if (o instanceof Notice) {
            if (add(new Metadata(profileId, TYPE_NOTICE, ((Notice) o).id, seen, false, 0)) == -1) {
                updateSeen(profileId, TYPE_NOTICE, ((Notice) o).id, seen);
            }
        }
        if (o instanceof Event) {
            if (add(new Metadata(profileId, ((Event) o).type == Event.TYPE_HOMEWORK ? TYPE_HOMEWORK : TYPE_EVENT, ((Event) o).id, seen, false, 0)) == -1) {
                updateSeen(profileId, ((Event) o).type == Event.TYPE_HOMEWORK ? TYPE_HOMEWORK : TYPE_EVENT, ((Event) o).id, seen);
            }
        }
        if (o instanceof LessonChange) {
            if (add(new Metadata(profileId, TYPE_LESSON_CHANGE, ((LessonChange) o).id, seen, false, 0)) == -1) {
                updateSeen(profileId, TYPE_LESSON_CHANGE, ((LessonChange) o).id, seen);
            }
        }
        if (o instanceof LessonFull) {
            if (add(new Metadata(profileId, TYPE_LESSON_CHANGE, ((LessonFull) o).changeId, seen, false, 0)) == -1) {
                updateSeen(profileId, TYPE_LESSON_CHANGE, ((LessonFull) o).changeId, seen);
            }
        }
        if (o instanceof Announcement) {
            if (add(new Metadata(profileId, TYPE_ANNOUNCEMENT, ((Announcement) o).id, seen, false, 0)) == -1) {
                updateSeen(profileId, TYPE_ANNOUNCEMENT, ((Announcement) o).id, seen);
            }
        }
        if (o instanceof Message) {
            if (add(new Metadata(profileId, TYPE_MESSAGE, ((Message) o).id, seen, false, 0)) == -1) {
                updateSeen(profileId, TYPE_MESSAGE, ((Message) o).id, seen);
            }
        }
    }

    @Transaction
    public void setNotified(int profileId, Object o, boolean notified) {
        if (o instanceof Grade) {
            if (add(new Metadata(profileId, TYPE_GRADE, ((Grade) o).id, false, notified, 0)) == -1) {
                updateNotified(profileId, TYPE_GRADE, ((Grade) o).id, notified);
            }
        }
        if (o instanceof Attendance) {
            if (add(new Metadata(profileId, TYPE_ATTENDANCE, ((Attendance) o).id, false, notified, 0)) == -1) {
                updateNotified(profileId, TYPE_ATTENDANCE, ((Attendance) o).id, notified);
            }
        }
        if (o instanceof Notice) {
            if (add(new Metadata(profileId, TYPE_NOTICE, ((Notice) o).id, false, notified, 0)) == -1) {
                updateNotified(profileId, TYPE_NOTICE, ((Notice) o).id, notified);
            }
        }
        if (o instanceof Event) {
            if (add(new Metadata(profileId, ((Event) o).type == Event.TYPE_HOMEWORK ? TYPE_HOMEWORK : TYPE_EVENT, ((Event) o).id, false, notified, 0)) == -1) {
                updateNotified(profileId, ((Event) o).type == Event.TYPE_HOMEWORK ? TYPE_HOMEWORK : TYPE_EVENT, ((Event) o).id, notified);
            }
        }
        if (o instanceof LessonChange) {
            if (add(new Metadata(profileId, TYPE_LESSON_CHANGE, ((LessonChange) o).id, false, notified, 0)) == -1) {
                updateNotified(profileId, TYPE_LESSON_CHANGE, ((LessonChange) o).id, notified);
            }
        }
        if (o instanceof LessonFull) {
            if (add(new Metadata(profileId, TYPE_LESSON_CHANGE, ((LessonFull) o).changeId, false, notified, 0)) == -1) {
                updateNotified(profileId, TYPE_LESSON_CHANGE, ((LessonFull) o).changeId, notified);
            }
        }
        if (o instanceof Announcement) {
            if (add(new Metadata(profileId, TYPE_ANNOUNCEMENT, ((Announcement) o).id, false, notified, 0)) == -1) {
                updateNotified(profileId, TYPE_ANNOUNCEMENT, ((Announcement) o).id, notified);
            }
        }
        if (o instanceof Message) {
            if (add(new Metadata(profileId, TYPE_MESSAGE, ((Message) o).id, false, notified, 0)) == -1) {
                updateNotified(profileId, TYPE_MESSAGE, ((Message) o).id, notified);
            }
        }
    }

    @Transaction
    public void setBoth(int profileId, Event o, boolean seen, boolean notified, long addedDate) {
        if (o != null) {
            if (add(new Metadata(profileId, o.type == Event.TYPE_HOMEWORK ? TYPE_HOMEWORK : TYPE_EVENT, o.id, seen, notified, addedDate)) == -1) {
                updateSeen(profileId, o.type == Event.TYPE_HOMEWORK ? TYPE_HOMEWORK : TYPE_EVENT, o.id, seen);
                updateNotified(profileId, o.type == Event.TYPE_HOMEWORK ? TYPE_HOMEWORK : TYPE_EVENT, o.id, notified);
            }
        }
    }



    @Query("UPDATE metadata SET seen = :seen WHERE profileId = :profileId AND thingType = :thingType")
    public abstract void setAllSeen(int profileId, int thingType, boolean seen);

    @Query("UPDATE metadata SET notified = :notified WHERE profileId = :profileId AND thingType = :thingType")
    public abstract void setAllNotified(int profileId, int thingType, boolean notified);

    @Query("UPDATE metadata SET seen = :seen WHERE profileId = :profileId")
    public abstract void setAllSeen(int profileId, boolean seen);

    @Query("UPDATE metadata SET notified = :notified WHERE profileId = :profileId")
    public abstract void setAllNotified(int profileId, boolean notified);



    @Query("SELECT count() FROM metadata WHERE profileId = :profileId AND thingType = :thingType AND seen = 0")
    public abstract LiveData<Integer> countUnseen(int profileId, int thingType);

    @Query("SELECT count() FROM metadata WHERE profileId = :profileId AND thingType = :thingType AND seen = 0")
    public abstract Integer countUnseenNow(int profileId, int thingType);

    @Query("SELECT count() FROM metadata WHERE profileId = :profileId AND seen = 0")
    public abstract LiveData<Integer> countUnseen(int profileId);

    @Query("SELECT count() FROM metadata WHERE profileId = :profileId AND seen = 0")
    public abstract Integer countUnseenNow(int profileId);

    @Query("SELECT count() FROM metadata WHERE seen = 0")
    public abstract LiveData<Integer> countUnseen();



    @Query("DELETE FROM metadata WHERE profileId = :profileId AND thingType = :thingType AND thingId = :thingId")
    public abstract void delete(int profileId, int thingType, long thingId);

    @Query("DELETE FROM metadata WHERE profileId = :profileId")
    public abstract void deleteAll(int profileId);



    @Query("SELECT profileId, thingType, COUNT(thingId) AS count FROM metadata WHERE seen = 0 GROUP BY profileId, thingType")
    public abstract LiveData<List<UnreadCounter>> getUnreadCounts();



    @Query("DELETE FROM metadata WHERE profileId = :profileId AND thingType = "+TYPE_GRADE+" AND thingId NOT IN (SELECT gradeId FROM grades WHERE profileId = :profileId);")
    public abstract void deleteUnusedGrades(int profileId);

    @Query("DELETE FROM metadata WHERE profileId = :profileId AND thingType = "+TYPE_NOTICE+" AND thingId NOT IN (SELECT noticeId FROM notices WHERE profileId = :profileId);")
    public abstract void deleteUnusedNotices(int profileId);

    @Query("DELETE FROM metadata WHERE profileId = :profileId AND thingType = "+TYPE_ATTENDANCE+" AND thingId NOT IN (SELECT attendanceId FROM attendances WHERE profileId = :profileId);")
    public abstract void deleteUnusedAttendance(int profileId);

    @Query("DELETE FROM metadata WHERE profileId = :profileId AND thingType = "+TYPE_EVENT+" AND thingId NOT IN (SELECT eventId FROM events WHERE profileId = :profileId AND eventType != -1);")
    public abstract void deleteUnusedEvents(int profileId);

    @Query("DELETE FROM metadata WHERE profileId = :profileId AND thingType = "+TYPE_HOMEWORK+" AND thingId NOT IN (SELECT eventId FROM events WHERE profileId = :profileId AND eventType = -1);")
    public abstract void deleteUnusedHomework(int profileId);

    @Query("DELETE FROM metadata WHERE profileId = :profileId AND thingType = "+TYPE_LESSON_CHANGE+" AND thingId NOT IN (SELECT lessonChangeId FROM lessonChanges WHERE profileId = :profileId);")
    public abstract void deleteUnusedLessonChanges(int profileId);

    @Query("DELETE FROM metadata WHERE profileId = :profileId AND thingType = "+TYPE_ANNOUNCEMENT+" AND thingId NOT IN (SELECT announcementId FROM announcements WHERE profileId = :profileId);")
    public abstract void deleteUnusedAnnouncements(int profileId);

    @Query("DELETE FROM metadata WHERE profileId = :profileId AND thingType = "+TYPE_MESSAGE+" AND thingId NOT IN (SELECT messageId FROM messages WHERE profileId = :profileId);")
    public abstract void deleteUnusedMessages(int profileId);

    @Transaction
    public void deleteUnused(int profileId) {
        deleteUnusedGrades(profileId);
        deleteUnusedNotices(profileId);
        deleteUnusedAttendance(profileId);
        deleteUnusedEvents(profileId);
        deleteUnusedHomework(profileId);
        deleteUnusedLessonChanges(profileId);
        deleteUnusedAnnouncements(profileId);
        deleteUnusedMessages(profileId);
    }
}

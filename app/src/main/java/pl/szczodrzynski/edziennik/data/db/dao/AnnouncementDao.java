/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.List;

import pl.szczodrzynski.edziennik.data.db.entity.Announcement;
import pl.szczodrzynski.edziennik.data.db.entity.Metadata;
import pl.szczodrzynski.edziennik.data.db.full.AnnouncementFull;

import static pl.szczodrzynski.edziennik.data.db.entity.Metadata.TYPE_ANNOUNCEMENT;

@Dao
public abstract class AnnouncementDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract long add(Announcement announcement);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void addAll(List<Announcement> announcementList);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void addAllIgnore(List<Announcement> announcementList);

    @Query("DELETE FROM announcements WHERE profileId = :profileId")
    public abstract void clear(int profileId);

    @RawQuery(observedEntities = {Announcement.class, Metadata.class})
    abstract LiveData<List<AnnouncementFull>> getAll(SupportSQLiteQuery query);
    public LiveData<List<AnnouncementFull>> getAll(int profileId, String filter) {
        return getAll(new SimpleSQLiteQuery("SELECT \n" +
                "*, \n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS teacherFullName\n" +
                "FROM announcements \n" +
                "LEFT JOIN teachers USING(profileId, teacherId)\n" +
                "LEFT JOIN metadata ON announcementId = thingId AND thingType = "+TYPE_ANNOUNCEMENT+" AND metadata.profileId = "+profileId+"\n" +
                "WHERE announcements.profileId = "+profileId+" AND "+filter+"\n" +
                "ORDER BY addedDate DESC"));
    }
    public LiveData<List<AnnouncementFull>> getAll(int profileId) {
        return getAll(profileId, "1");
    }
    public LiveData<List<AnnouncementFull>> getAllWhere(int profileId, String filter) {
        return getAll(profileId, filter);
    }

    @RawQuery(observedEntities = {Announcement.class, Metadata.class})
    abstract List<AnnouncementFull> getAllNow(SupportSQLiteQuery query);
    public List<AnnouncementFull> getAllNow(int profileId, String filter) {
        return getAllNow(new SimpleSQLiteQuery("SELECT \n" +
                "*, \n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS teacherFullName\n" +
                "FROM announcements \n" +
                "LEFT JOIN teachers USING(profileId, teacherId)\n" +
                "LEFT JOIN metadata ON announcementId = thingId AND thingType = "+TYPE_ANNOUNCEMENT+" AND metadata.profileId = "+profileId+"\n" +
                "WHERE announcements.profileId = "+profileId+" AND "+filter+"\n" +
                "ORDER BY addedDate DESC"));
    }
    public List<AnnouncementFull> getNotNotifiedNow(int profileId) {
        return getAllNow(profileId, "notified = 0");
    }

    @Query("SELECT " +
            "*, " +
            "teachers.teacherName || ' ' || teachers.teacherSurname AS teacherFullName " +
            "FROM announcements " +
            "LEFT JOIN teachers USING(profileId, teacherId) " +
            "LEFT JOIN metadata ON announcementId = thingId AND thingType = "+TYPE_ANNOUNCEMENT+" AND metadata.profileId = announcements.profileId " +
            "WHERE notified = 0 " +
            "ORDER BY addedDate DESC")
    public abstract List<AnnouncementFull> getNotNotifiedNow();
}

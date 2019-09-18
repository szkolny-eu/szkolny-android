package pl.szczodrzynski.edziennik.datamodels;

import androidx.lifecycle.LiveData;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RawQuery;

import java.util.List;

import static pl.szczodrzynski.edziennik.datamodels.Metadata.TYPE_NOTICE;

@Dao
public abstract class NoticeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract long add(Notice notice);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void addAll(List<Notice> noticeList);

    @Query("DELETE FROM notices WHERE profileId = :profileId")
    public abstract void clear(int profileId);

    @Query("DELETE FROM notices WHERE profileId = :profileId AND noticeSemester = :semester")
    public abstract void clearForSemester(int profileId, int semester);

    @RawQuery(observedEntities = {Notice.class})
    abstract LiveData<List<NoticeFull>> getAll(SupportSQLiteQuery query);
    public LiveData<List<NoticeFull>> getAll(int profileId, String filter) {
        return getAll(new SimpleSQLiteQuery("SELECT \n" +
                "*, \n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS teacherFullName\n" +
                "FROM notices \n" +
                "LEFT JOIN teachers USING(profileId, teacherId)\n" +
                "LEFT JOIN metadata ON noticeId = thingId AND thingType = "+TYPE_NOTICE+" AND metadata.profileId = "+profileId+"\n" +
                "WHERE notices.profileId = "+profileId+" AND "+filter+"\n" +
                "ORDER BY addedDate DESC"));
    }
    public LiveData<List<NoticeFull>> getAll(int profileId) {
        return getAll(profileId, "1");
    }
    public LiveData<List<NoticeFull>> getAllWhere(int profileId, String filter) {
        return getAll(profileId, filter);
    }

    @RawQuery(observedEntities = {Notice.class, Metadata.class})
    abstract List<NoticeFull> getAllNow(SupportSQLiteQuery query);
    public List<NoticeFull> getAllNow(int profileId, String filter) {
        return getAllNow(new SimpleSQLiteQuery("SELECT \n" +
                "*, \n" +
                "teachers.teacherName || ' ' || teachers.teacherSurname AS teacherFullName\n" +
                "FROM notices \n" +
                "LEFT JOIN teachers USING(profileId, teacherId)\n" +
                "LEFT JOIN metadata ON noticeId = thingId AND thingType = "+TYPE_NOTICE+" AND metadata.profileId = "+profileId+"\n" +
                "WHERE notices.profileId = "+profileId+" AND "+filter+"\n" +
                "ORDER BY addedDate DESC"));
    }
    public List<NoticeFull> getNotNotifiedNow(int profileId) {
        return getAllNow(profileId, "notified = 0");
    }
}

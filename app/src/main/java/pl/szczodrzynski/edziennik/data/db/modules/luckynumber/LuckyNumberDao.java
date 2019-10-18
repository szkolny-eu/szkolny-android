package pl.szczodrzynski.edziennik.data.db.modules.luckynumber;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.RawQuery;
import androidx.sqlite.db.SimpleSQLiteQuery;
import androidx.sqlite.db.SupportSQLiteQuery;

import java.util.List;

import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata;
import pl.szczodrzynski.edziennik.data.db.modules.notices.Notice;
import pl.szczodrzynski.edziennik.utils.models.Date;

import static pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata.TYPE_LUCKY_NUMBER;

@Dao
public abstract class LuckyNumberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void add(LuckyNumber luckyNumber);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void addAll(List<LuckyNumber> luckyNumberList);

    @Query("DELETE FROM luckyNumbers WHERE profileId = :profileId")
    public abstract void clear(int profileId);

    @Query("SELECT * FROM luckyNumbers WHERE profileId = :profileId AND luckyNumberDate = :date")
    public abstract LiveData<LuckyNumber> getByDate(int profileId, Date date);

    @Query("SELECT * FROM luckyNumbers WHERE profileId = :profileId AND luckyNumberDate = :date")
    public abstract LuckyNumber getByDateNow(int profileId, Date date);

    @Nullable
    @Query("SELECT * FROM luckyNumbers WHERE profileId = :profileId AND luckyNumberDate > :date ORDER BY luckyNumberDate ASC LIMIT 1")
    public abstract LuckyNumber getNearestFutureNow(int profileId, Date date);

    @RawQuery(observedEntities = {LuckyNumber.class})
    abstract LiveData<List<LuckyNumberFull>> getAll(SupportSQLiteQuery query);
    public LiveData<List<LuckyNumberFull>> getAll(int profileId, String filter) {
        return getAll(new SimpleSQLiteQuery("SELECT\n" +
                "*\n" +
                "FROM luckyNumbers\n" +
                "LEFT JOIN metadata ON luckyNumberDate = thingId AND thingType = "+TYPE_LUCKY_NUMBER+" AND metadata.profileId = "+profileId+"\n" +
                "WHERE luckyNumbers.profileId = "+profileId+" AND "+filter+"\n" +
                "ORDER BY addedDate DESC"));
    }
    public LiveData<List<LuckyNumberFull>> getAll(int profileId) {
        return getAll(profileId, "1");
    }
    public LiveData<List<LuckyNumberFull>> getAllWhere(int profileId, String filter) {
        return getAll(profileId, filter);
    }

    @RawQuery(observedEntities = {Notice.class, Metadata.class})
    abstract List<LuckyNumberFull> getAllNow(SupportSQLiteQuery query);
    public List<LuckyNumberFull> getAllNow(int profileId, String filter) {
        return getAllNow(new SimpleSQLiteQuery("SELECT\n" +
                "*\n" +
                "FROM luckyNumbers\n" +
                "LEFT JOIN metadata ON luckyNumberDate = thingId AND thingType = "+TYPE_LUCKY_NUMBER+" AND metadata.profileId = "+profileId+"\n" +
                "WHERE luckyNumbers.profileId = "+profileId+" AND "+filter+"\n" +
                "ORDER BY addedDate DESC"));
    }
    public List<LuckyNumberFull> getNotNotifiedNow(int profileId) {
        return getAllNow(profileId, "notified = 0");
    }
}

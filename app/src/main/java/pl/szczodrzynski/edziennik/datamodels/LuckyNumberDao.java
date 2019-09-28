package pl.szczodrzynski.edziennik.datamodels;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

import pl.szczodrzynski.edziennik.utils.models.Date;

@Dao
public interface LuckyNumberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void add(LuckyNumber luckyNumber);

    @Query("DELETE FROM luckyNumbers WHERE profileId = :profileId")
    void clear(int profileId);

    @Query("SELECT * FROM luckyNumbers WHERE profileId = :profileId AND luckyNumberDate = :date")
    LiveData<LuckyNumber> getByDate(int profileId, Date date);

    @Query("SELECT * FROM luckyNumbers WHERE profileId = :profileId AND luckyNumberDate = :date")
    LuckyNumber getByDateNow(int profileId, Date date);

    @Query("SELECT * FROM luckyNumbers WHERE profileId = :profileId ORDER BY luckyNumberDate DESC")
    LiveData<List<LuckyNumber>> getAll(int profileId);

    @Query("SELECT * FROM luckyNumbers WHERE profileId = :profileId ORDER BY luckyNumberDate DESC")
    List<LuckyNumber> getAllNow(int profileId);
}

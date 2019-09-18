package pl.szczodrzynski.edziennik.datamodels;

import androidx.room.Dao;
import androidx.room.Insert;

@Dao
public interface DebugLogDao {
    @Insert
    void add(DebugLog debugLog);
}

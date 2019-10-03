package pl.szczodrzynski.edziennik.data.db.modules.debuglog;

import androidx.room.Dao;
import androidx.room.Insert;

@Dao
public interface DebugLogDao {
    @Insert
    void add(DebugLog debugLog);
}

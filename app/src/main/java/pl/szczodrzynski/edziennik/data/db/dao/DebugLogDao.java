/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;

import pl.szczodrzynski.edziennik.data.db.entity.DebugLog;

@Dao
public interface DebugLogDao {
    @Insert
    void add(DebugLog debugLog);
}

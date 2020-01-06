/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.data.db.dao;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import pl.szczodrzynski.edziennik.data.db.entity.EventType;

@Dao
public interface EventTypeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void add(EventType gradeCategory);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addAll(List<EventType> gradeCategoryList);

    @Query("DELETE FROM eventTypes WHERE profileId = :profileId")
    void clear(int profileId);

    @Query("SELECT * FROM eventTypes WHERE profileId = :profileId AND eventType = :typeId")
    EventType getByIdNow(int profileId, int typeId);

    @Query("SELECT * FROM eventTypes WHERE profileId = :profileId")
    LiveData<List<EventType>> getAll(int profileId);

    @Query("SELECT * FROM eventTypes WHERE profileId = :profileId")
    List<EventType> getAllNow(int profileId);
}

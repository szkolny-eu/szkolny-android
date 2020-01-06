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

import pl.szczodrzynski.edziennik.data.db.entity.FeedbackMessage;
import pl.szczodrzynski.edziennik.data.db.full.FeedbackMessageWithCount;

@Dao
public interface FeedbackMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void add(FeedbackMessage feedbackMessage);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addAll(List<FeedbackMessage> feedbackMessageList);

    @Query("DELETE FROM feedbackMessages")
    void clear();

    @Query("SELECT * FROM feedbackMessages")
    List<FeedbackMessage> getAllNow();

    @Query("SELECT * FROM feedbackMessages WHERE fromUser = :fromUser")
    List<FeedbackMessage> getAllByUserNow(String fromUser);

    @Query("SELECT * FROM feedbackMessages")
    LiveData<List<FeedbackMessage>> getAll();

    @Query("SELECT *, COUNT(*) AS messageCount FROM feedbackMessages GROUP BY fromUser ORDER BY sentTime DESC")
    List<FeedbackMessageWithCount> getAllWithCountNow();
}

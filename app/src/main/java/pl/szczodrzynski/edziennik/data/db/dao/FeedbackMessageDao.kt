/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */
package pl.szczodrzynski.edziennik.data.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import pl.szczodrzynski.edziennik.data.db.entity.FeedbackMessage

@Dao
interface FeedbackMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(feedbackMessage: FeedbackMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAll(feedbackMessageList: List<FeedbackMessage>)

    @Query("DELETE FROM feedbackMessages")
    fun clear()

    @get:Query("SELECT * FROM feedbackMessages ORDER BY sentTime DESC LIMIT 50")
    val allNow: List<FeedbackMessage>

    @Query("SELECT * FROM feedbackMessages WHERE deviceId = :deviceId ORDER BY sentTime DESC LIMIT 50")
    fun getByDeviceIdNow(deviceId: String): List<FeedbackMessage>

    @get:Query("SELECT * FROM feedbackMessages")
    val all: LiveData<List<FeedbackMessage>>

    @get:Query("SELECT *, COUNT(*) AS count FROM feedbackMessages WHERE received = 1 AND devId IS NULL AND deviceId != 'szkolny.eu' GROUP BY deviceId ORDER BY sentTime DESC")
    val allWithCountNow: List<FeedbackMessage.WithCount>
}

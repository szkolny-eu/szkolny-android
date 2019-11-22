/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-18.
 */

package pl.szczodrzynski.edziennik.data.db.modules.notification

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NotificationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(notification: Notification)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAll(notificationList: List<Notification>)

    @Query("DELETE FROM notifications WHERE profileId = :profileId")
    fun clear(profileId: Int)

    @Query("DELETE FROM notifications")
    fun clearAll()

    @Query("SELECT * FROM notifications ORDER BY addedDate DESC")
    fun getAll(): LiveData<List<Notification>>

    @Query("SELECT * FROM notifications")
    fun getAllNow(): List<Notification>

    @Query("SELECT * FROM notifications WHERE posted = 0 ORDER BY addedDate DESC")
    fun getNotPostedNow(): List<Notification>

    @Query("UPDATE notifications SET posted = 1 WHERE posted = 0")
    fun setAllPosted()
}
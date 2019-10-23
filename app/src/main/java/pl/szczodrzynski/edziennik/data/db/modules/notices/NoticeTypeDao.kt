/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-23.
 */

package pl.szczodrzynski.edziennik.data.db.modules.notices

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NoticeTypeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(noticeType: NoticeType)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAll(noticeTypeList: List<NoticeType>)

    @Query("DELETE FROM noticeTypes WHERE profileId = :profileId")
    fun clear(profileId: Int)

    @Query("SELECT * FROM noticeTypes WHERE profileId = :profileId ORDER BY id ASC")
    fun getAllNow(profileId: Int): List<NoticeType>
}

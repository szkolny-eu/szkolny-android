/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.data.db.modules.lessons

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LessonRangeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(lessonRange: LessonRange)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAll(lessonRangeList: List<LessonRange>)

    @Query("SELECT * FROM lessonRanges WHERE profileId = :profileId")
    fun getAllNow(profileId: Int): List<LessonRange>

    @Query("DELETE FROM lessonRanges WHERE profileId = :profileId")
    fun clear(profileId: Int)
}
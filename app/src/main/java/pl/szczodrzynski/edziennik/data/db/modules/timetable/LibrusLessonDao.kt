/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-6.
 */

package pl.szczodrzynski.edziennik.data.db.modules.timetable

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LibrusLessonDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun add(librusLesson: LibrusLesson)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun addAll(librusLessonList: List<LibrusLesson>)

    @Query("SELECT * FROM librusLessons WHERE profileId = :profileId")
    fun getAllNow(profileId: Int): List<LibrusLesson>
}

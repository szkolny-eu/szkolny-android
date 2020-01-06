/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-5.
 */

package pl.szczodrzynski.edziennik.data.db.modules.timetable

import androidx.room.Entity
import androidx.room.Index

@Entity(tableName = "librusLessons",
        primaryKeys = ["profileId", "lessonId"],
        indices = [Index("profileId")])
data class LibrusLesson(
        val profileId: Int,
        val lessonId: Long,
        val teacherId: Long,
        val subjectId: Long,
        val teamId: Long? = null
)

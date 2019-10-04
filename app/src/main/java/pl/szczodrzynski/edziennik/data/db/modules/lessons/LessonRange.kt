/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-4.
 */

package pl.szczodrzynski.edziennik.data.db.modules.lessons

import androidx.room.ColumnInfo
import androidx.room.Entity
import pl.szczodrzynski.edziennik.utils.models.Time

@Entity(tableName = "lessonRanges",
        primaryKeys = ["profileId", "lessonRangeNumber"])
class LessonRange (

        val profileId: Int,

        @ColumnInfo(name = "lessonRangeNumber")
        val lessonNumber: Int,

        @ColumnInfo(name = "lessonRangeStart")
        val startTime: Time,

        @ColumnInfo(name = "lessonRangeEnd")
        val endTime: Time
)
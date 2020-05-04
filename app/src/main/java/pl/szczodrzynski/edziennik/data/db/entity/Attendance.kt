/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-24.
 */
package pl.szczodrzynski.edziennik.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

@Entity(tableName = "attendances",
        primaryKeys = ["profileId", "attendanceId"],
        indices = [
            Index(value = ["profileId"])
        ])
open class Attendance(
        val profileId: Int,
        @ColumnInfo(name = "attendanceId")
        var id: Long,
        /** Base type ID used to count attendance stats */
        @ColumnInfo(name = "attendanceBaseType")
        var baseType: Int,
        /** A full type name coming from the e-register */
        @ColumnInfo(name = "attendanceTypeName")
        var typeName: String,
        /** A short name to display by default, might be different for non-standard types */
        @ColumnInfo(name = "attendanceTypeShort")
        val typeShort: String,
        /** A short name that the e-register would display */
        @ColumnInfo(name = "attendanceTypeSymbol")
        var typeSymbol: String,
        /** A color that the e-register would display, null falls back to app's default */
        @ColumnInfo(name = "attendanceTypeColor")
        var typeColor: Int?,

        @ColumnInfo(name = "attendanceDate")
        var date: Date,
        @ColumnInfo(name = "attendanceTime")
        var startTime: Time?,
        @ColumnInfo(name = "attendanceSemester")
        var semester: Int,

        var teacherId: Long,
        var subjectId: Long,
        var addedDate: Long = System.currentTimeMillis()
) : Keepable() {
    companion object {
        const val TYPE_UNKNOWN = -1         // #3f51b5
        const val TYPE_PRESENT = 0          // #009688
        const val TYPE_PRESENT_CUSTOM = 10  // count as presence AND show in the list + custom color, fallback: #3f51b5
        const val TYPE_ABSENT = 1           // #ff3d00
        const val TYPE_ABSENT_EXCUSED = 2   // #76ff03
        const val TYPE_RELEASED = 3         // #9e9e9e
        const val TYPE_BELATED = 4          // #ffc107
        const val TYPE_BELATED_EXCUSED = 5  // #ffc107
        const val TYPE_DAY_FREE = 6         // #43a047

        // attendance bar order:
        // day_free, present, present_custom, unknown, belated_excused, belated, released, absent_excused, absent,
    }

    @ColumnInfo(name = "attendanceLessonTopic")
    var lessonTopic: String? = null
    @ColumnInfo(name = "attendanceLessonNumber")
    var lessonNumber: Int? = null

    @Ignore
    var showAsUnseen: Boolean? = null
}

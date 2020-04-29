/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-25.
 */
package pl.szczodrzynski.edziennik.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Index
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

@Entity(tableName = "teacherAbsence",
        primaryKeys = ["profileId", "teacherAbsenceId"],
        indices = [
            Index(value = ["profileId"])
        ])
open class TeacherAbsence(
        val profileId: Int,
        @ColumnInfo(name = "teacherAbsenceId")
        val id: Long,
        @ColumnInfo(name = "teacherAbsenceType")
        val type: Long,
        @ColumnInfo(name = "teacherAbsenceName")
        val name: String?,

        @ColumnInfo(name = "teacherAbsenceDateFrom")
        val dateFrom: Date,
        @ColumnInfo(name = "teacherAbsenceDateTo")
        val dateTo: Date,
        @ColumnInfo(name = "teacherAbsenceTimeFrom")
        val timeFrom: Time?,
        @ColumnInfo(name = "teacherAbsenceTimeTo")
        val timeTo: Time?,

        val teacherId: Long,
        var addedDate: Long = System.currentTimeMillis()
) : Keepable() {
    @Ignore
    var showAsUnseen = false
}

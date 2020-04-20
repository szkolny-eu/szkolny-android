/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-18
 */

package pl.szczodrzynski.edziennik.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = "teacherAbsenceTypes",
        primaryKeys = ["profileId", "teacherAbsenceTypeId"])
data class TeacherAbsenceType (
        val profileId: Int,

        @ColumnInfo(name = "teacherAbsenceTypeId")
        val id: Long,

        @ColumnInfo(name = "teacherAbsenceTypeName")
        val name: String
)

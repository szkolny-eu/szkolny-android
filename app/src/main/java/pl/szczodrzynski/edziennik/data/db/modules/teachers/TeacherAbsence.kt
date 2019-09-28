package pl.szczodrzynski.edziennik.data.db.modules.teachers

import androidx.room.ColumnInfo
import androidx.room.Entity
import pl.szczodrzynski.edziennik.utils.models.Date

@Entity(tableName = "teacherAbsence",
        primaryKeys = ["profileId", "teacherAbsenceId"])
open class TeacherAbsence (

    val profileId: Int,

    @ColumnInfo(name = "teacherAbsenceId")
    val id: Long,

    val teacherId: Long,

    @ColumnInfo(name = "teacherAbsenceType")
    val type: Long,

    @ColumnInfo(name = "teacherAbsenceDateFrom")
    val dateFrom: Date,

    @ColumnInfo(name = "teacherAbsenceDateTo")
    val dateTo: Date

)

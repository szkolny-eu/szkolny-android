package pl.szczodrzynski.edziennik.ui.modules.agenda.teacherabsence

import pl.szczodrzynski.edziennik.utils.models.Date
import java.util.*

class TeacherAbsenceCounter (
    val teacherAbsenceDate: Date,
    var teacherAbsenceCount: Int = 0
) {
    val startTime: Calendar
        get() = Calendar.getInstance().apply {
            set(teacherAbsenceDate.year, teacherAbsenceDate.month - 1, teacherAbsenceDate.day, 10, 0, 0)
        }

    val endTime: Calendar
        get() = Calendar.getInstance().apply {
            timeInMillis = startTime.timeInMillis + (45 * 60 * 1000)
        }
}

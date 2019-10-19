package pl.szczodrzynski.edziennik.data.db.modules.teachers

import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class TeacherAbsenceFull(profileId: Int, id: Long, teacherId: Long, type: Long, name: String?,
                         dateFrom: Date, dateTo: Date, timeFrom: Time?, timeTo: Time?)
    : TeacherAbsence(profileId, id, teacherId, type, name, dateFrom, dateTo, timeFrom, timeTo) {

    var teacherFullName = ""

    // metadata
    var seen: Boolean = false
    var notified: Boolean = false
    var addedDate: Long = 0
}

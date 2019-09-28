package pl.szczodrzynski.edziennik.data.db.modules.teachers

import pl.szczodrzynski.edziennik.utils.models.Date

class TeacherAbsenceFull(profileId: Int, id: Long, teacherId: Long, type: Long, dateFrom: Date, dateTo: Date)
    : TeacherAbsence(profileId, id, teacherId, type, dateFrom, dateTo) {

    var teacherFullName = ""

    // metadata
    var seen: Boolean = false
    var notified: Boolean = false
    var addedDate: Long = 0
}

/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */
package pl.szczodrzynski.edziennik.data.db.full

import pl.szczodrzynski.edziennik.data.db.entity.Grade

class GradeFull(
        profileId: Int, id: Long, name: String, type: Int,
        value: Float, weight: Float, color: Int,
        category: String?, description: String?, comment: String?,
        semester: Int, teacherId: Long, subjectId: Long
) : Grade(
        profileId, id, name, type,
        value, weight, color,
        category, description, comment,
        semester, teacherId, subjectId
) {
    var subjectLongName: String? = null
    var subjectShortName: String? = null
    var teacherFullName: String? = null
    // metadata
    var seen = false
    var notified = false
    var addedDate: Long = 0
}

/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-24.
 */
package pl.szczodrzynski.edziennik.data.db.full

import pl.szczodrzynski.edziennik.data.db.entity.Grade

class GradeFull(
        profileId: Int, id: Long, name: String, type: Int,
        value: Float, weight: Float, color: Int,
        category: String?, description: String?, comment: String?,
        semester: Int, teacherId: Long, subjectId: Long, addedDate: Long = System.currentTimeMillis()
) : Grade(
        profileId, id, name, type,
        value, weight, color,
        category, description, comment,
        semester, teacherId, subjectId, addedDate
) {
    var teacherName: String? = null
    var subjectLongName: String? = null
    var subjectShortName: String? = null

    // metadata
    var seen = false
    var notified = false
}

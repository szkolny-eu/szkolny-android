/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-25.
 */
package pl.szczodrzynski.edziennik.data.db.full

import pl.szczodrzynski.edziennik.data.db.entity.Announcement
import pl.szczodrzynski.edziennik.utils.models.Date

class AnnouncementFull(
        profileId: Int, id: Long,
        subject: String, text: String?,
        startDate: Date?, endDate: Date?,
        teacherId: Long, addedDate: Long = System.currentTimeMillis()
) : Announcement(
        profileId, id,
        subject, text,
        startDate, endDate,
        teacherId, addedDate
) {
    var teacherName: String? = null

    // metadata
    var seen = false
    var notified = false
}

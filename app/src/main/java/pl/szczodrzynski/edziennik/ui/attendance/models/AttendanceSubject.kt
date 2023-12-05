/*
 * Copyright (c) Kuba Szczodrzyński 2020-5-4.
 */

package pl.szczodrzynski.edziennik.ui.attendance.models

import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.data.db.entity.AttendanceType
import pl.szczodrzynski.edziennik.data.db.full.AttendanceFull
import pl.szczodrzynski.edziennik.ui.grades.models.ExpandableItemModel

data class AttendanceSubject(
        val subjectId: Long,
        val subjectName: String,
        override val items: MutableList<AttendanceFull> = mutableListOf()
) : ExpandableItemModel<AttendanceFull>(items) {
    override var level = 1

    var lastAddedDate = 0L

    var hasUnseen: Boolean = false
        get() = field || items.any { it.baseType != Attendance.TYPE_PRESENT && !it.seen }

    var typeCountMap: Map<AttendanceType, Int> = mapOf()
    var percentage: Float = 0f
    var presenceDifference: Int = 0
}

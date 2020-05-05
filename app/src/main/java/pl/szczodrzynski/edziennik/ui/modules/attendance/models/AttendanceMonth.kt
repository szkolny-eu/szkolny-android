/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-30.
 */

package pl.szczodrzynski.edziennik.ui.modules.attendance.models

import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.data.db.entity.AttendanceType
import pl.szczodrzynski.edziennik.data.db.full.AttendanceFull
import pl.szczodrzynski.edziennik.ui.modules.grades.models.ExpandableItemModel

data class AttendanceMonth(
        val year: Int,
        val month: Int,
        override val items: MutableList<AttendanceFull> = mutableListOf()
) : ExpandableItemModel<AttendanceFull>(items) {
    override var level = 1

    var lastAddedDate = 0L

    var hasUnseen: Boolean = false
        get() = field || items.any { it.baseType != Attendance.TYPE_PRESENT && !it.seen }

    var typeCountMap: Map<AttendanceType, Int> = mapOf()
    var percentage: Float = 0f
}

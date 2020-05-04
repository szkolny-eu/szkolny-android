/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-30.
 */

package pl.szczodrzynski.edziennik.ui.modules.attendance.models

import pl.szczodrzynski.edziennik.data.db.entity.Attendance
import pl.szczodrzynski.edziennik.data.db.full.AttendanceFull
import pl.szczodrzynski.edziennik.ui.modules.grades.models.ExpandableItemModel
import pl.szczodrzynski.edziennik.utils.models.Date

data class AttendanceDayRange(
        var rangeStart: Date,
        var rangeEnd: Date?,
        override val items: MutableList<AttendanceFull> = mutableListOf()
) : ExpandableItemModel<AttendanceFull>(items) {
    override var level = 1

    var lastAddedDate = 0L

    var hasUnseen: Boolean = false
        get() = field || items.any { it.baseType != Attendance.TYPE_PRESENT && !it.seen }
}

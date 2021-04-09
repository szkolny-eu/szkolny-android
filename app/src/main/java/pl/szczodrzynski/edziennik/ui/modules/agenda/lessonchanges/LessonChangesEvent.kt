/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-8.
 */

package pl.szczodrzynski.edziennik.ui.modules.agenda.lessonchanges

import pl.szczodrzynski.edziennik.ui.modules.agenda.BaseEvent
import pl.szczodrzynski.edziennik.utils.models.Date

class LessonChangesEvent(
    val profileId: Int,
    val date: Date,
    val changeCount: Int
) : BaseEvent(
    id = date.value.toLong(),
    time = date.asCalendar,
    color = 0xff78909c.toInt(),
    showBadge = false
) {
    override fun copy() = LessonChangesEvent(profileId, date, changeCount)
}

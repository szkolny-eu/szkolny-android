/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-8.
 */

package pl.szczodrzynski.edziennik.ui.agenda.lessonchanges

import pl.szczodrzynski.edziennik.ui.agenda.BaseEvent
import pl.szczodrzynski.edziennik.utils.models.Date

class LessonChangesEvent(
    val profileId: Int,
    val date: Date,
    val count: Int,
    showBadge: Boolean
) : BaseEvent(
    id = date.value.toLong(),
    time = date.asCalendar,
    color = 0xff78909c.toInt(),
    showBadge = false,
    showItemBadge = showBadge
) {
    override fun copy() = LessonChangesEvent(profileId, date, count, showItemBadge)

    override fun getShowBadge() = false
}

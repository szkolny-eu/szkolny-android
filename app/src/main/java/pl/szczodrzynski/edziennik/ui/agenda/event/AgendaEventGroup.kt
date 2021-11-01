/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-10.
 */

package pl.szczodrzynski.edziennik.ui.agenda.event

import pl.szczodrzynski.edziennik.ui.agenda.BaseEvent
import pl.szczodrzynski.edziennik.utils.models.Date

class AgendaEventGroup(
    val profileId: Int,
    val date: Date,
    val typeId: Long,
    val typeName: String,
    val typeColor: Int,
    val count: Int,
    showBadge: Boolean
) : BaseEvent(
    id = date.value.toLong(),
    time = date.asCalendar,
    color = typeColor,
    showBadge = showBadge
) {
    override fun copy() = AgendaEventGroup(profileId, date, typeId, typeName, typeColor, count, showBadge)
}

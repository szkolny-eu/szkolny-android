/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-10.
 */

package pl.szczodrzynski.edziennik.ui.modules.agenda.event

import pl.szczodrzynski.edziennik.ui.modules.agenda.BaseEvent
import pl.szczodrzynski.edziennik.utils.models.Date

class AgendaEventGroup(
    val profileId: Int,
    val date: Date,
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
    override fun copy() = AgendaEventGroup(profileId, date, typeName, typeColor, count, showBadge)
}

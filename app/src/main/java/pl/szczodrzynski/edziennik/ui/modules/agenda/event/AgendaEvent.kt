/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-8.
 */

package pl.szczodrzynski.edziennik.ui.modules.agenda.event

import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.ui.modules.agenda.BaseEvent

class AgendaEvent(
    val event: EventFull
) : BaseEvent(
    id = event.id,
    time = event.startTimeCalendar,
    color = event.eventColor,
    showBadge = !event.seen
) {
    override fun copy() = AgendaEvent(event)
}

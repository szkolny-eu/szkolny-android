/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-31.
 */

package pl.szczodrzynski.edziennik.data.api.events

import pl.szczodrzynski.edziennik.data.db.full.EventFull

data class EventGetEvent(val event: EventFull)

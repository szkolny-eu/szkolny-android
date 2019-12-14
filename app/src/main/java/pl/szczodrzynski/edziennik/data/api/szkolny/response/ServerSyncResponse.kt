/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-8
 */

package pl.szczodrzynski.edziennik.data.api.szkolny.response

import pl.szczodrzynski.edziennik.data.db.modules.events.EventFull

data class ServerSyncResponse(val events: List<EventFull>)

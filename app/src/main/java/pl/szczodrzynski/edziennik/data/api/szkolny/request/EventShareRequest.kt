/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-13
 */

package pl.szczodrzynski.edziennik.data.api.szkolny.request

import pl.szczodrzynski.edziennik.data.db.full.EventFull

data class EventShareRequest (

        val action: String = "event",
        val deviceId: String,

        val sharedByName: String,
        val shareTeamCode: String? = null,
        val unshareTeamCode: String? = null,
        val requesterName: String? = null,

        val eventId: Long? = null,
        val event: EventFull? = null
)

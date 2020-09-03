/*
 * Copyright (c) Kuba Szczodrzyński 2020-9-3.
 */

package pl.szczodrzynski.edziennik.data.api.events

import pl.szczodrzynski.edziennik.data.api.szkolny.response.RegisterAvailabilityStatus

data class RegisterAvailabilityEvent(
        val data: Map< String, RegisterAvailabilityStatus>
)

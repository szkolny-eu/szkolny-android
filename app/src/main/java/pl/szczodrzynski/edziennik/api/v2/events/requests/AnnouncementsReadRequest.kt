/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-26
 */

package pl.szczodrzynski.edziennik.api.v2.events.requests

import pl.szczodrzynski.edziennik.api.v2.models.ApiTask

data class AnnouncementsReadRequest(override val profileId: Int) : ApiTask(profileId) {
    override fun toString(): String {
        return "AnnouncementsReadRequest(profileId=$profileId)"
    }
}

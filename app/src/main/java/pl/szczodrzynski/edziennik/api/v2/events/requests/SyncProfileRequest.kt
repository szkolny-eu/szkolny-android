/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-28.
 */

package pl.szczodrzynski.edziennik.api.v2.events.requests

import pl.szczodrzynski.edziennik.api.v2.models.ApiTask

data class SyncProfileRequest(override val profileId: Int, val featureIds: List<Int>?) : ApiTask(profileId) {
    override fun toString(): String {
        return "SyncProfileRequest(profileId=$profileId, featureIds=$featureIds)"
    }
}
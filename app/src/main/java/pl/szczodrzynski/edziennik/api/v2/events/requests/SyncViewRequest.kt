/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-29.
 */

package pl.szczodrzynski.edziennik.api.v2.events.requests

import pl.szczodrzynski.edziennik.api.v2.models.ApiTask

class SyncViewRequest(override val profileId: Int, val targetId: Int, val targetType: Int) : ApiTask(profileId) {
    override fun toString(): String {
        return "SyncViewRequest(profileId=$profileId, targetId=$targetId, targetType=$targetType)"
    }
}
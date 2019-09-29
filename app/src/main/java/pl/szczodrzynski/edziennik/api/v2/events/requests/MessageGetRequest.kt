/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-28.
 */

package pl.szczodrzynski.edziennik.api.v2.events.requests

import pl.szczodrzynski.edziennik.api.v2.models.ApiTask

data class MessageGetRequest(override val profileId: Int, val messageId: Int) : ApiTask(profileId) {
    override fun toString(): String {
        return "MessageGetRequest(profileId=$profileId, messageId=$messageId)"
    }
}
/*
 * Copyright (c) Kuba Szczodrzyński 2021-2-22.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe

import com.google.gson.JsonPrimitive
import pl.szczodrzynski.edziennik.data.api.VULCAN_HEBE_ENDPOINT_PUSH_ALL
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_HEBE_PUSH_CONFIG
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanHebe
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS

class VulcanHebePushConfig(
    override val data: DataVulcan,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit
) : VulcanHebe(data, lastSync) {
    companion object {
        const val TAG = "VulcanHebePushConfig"
    }

    init {
        apiPost(
            TAG,
            VULCAN_HEBE_ENDPOINT_PUSH_ALL,
            payload = JsonPrimitive("on")
        ) { _: Boolean, _ ->
            // sync always: this endpoint has .shouldSync set
            data.setSyncNext(ENDPOINT_VULCAN_HEBE_PUSH_CONFIG, SYNC_ALWAYS)
            data.app.config.sync.tokenVulcanList =
                data.app.config.sync.tokenVulcanList + profileId
            onSuccess(ENDPOINT_VULCAN_HEBE_PUSH_CONFIG)
        }
    }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-20.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.api

import pl.szczodrzynski.edziennik.data.api.VULCAN_API_ENDPOINT_PUSH
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_API_PUSH_CONFIG
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanApi
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS

class VulcanApiPushConfig(override val data: DataVulcan,
                        override val lastSync: Long?,
                        val onSuccess: (endpointId: Int) -> Unit
) : VulcanApi(data, lastSync) {
    companion object {
        const val TAG = "VulcanApiPushConfig"
    }

    init { data.app.config.sync.tokenVulcan?.also { tokenVulcan ->
        apiGet(TAG, VULCAN_API_ENDPOINT_PUSH, parameters = mapOf(
                "Token" to tokenVulcan,
                "IdUczen" to data.studentId,
                "PushOcena" to true,
                "PushFrekwencja" to true,
                "PushUwaga" to true,
                "PushWiadomosc" to true
        )) { _, _ ->
            // sync always: this endpoint has .shouldSync set
            data.setSyncNext(ENDPOINT_VULCAN_API_PUSH_CONFIG, SYNC_ALWAYS)
            data.app.config.sync.tokenVulcanList =
                    data.app.config.sync.tokenVulcanList + profileId
            onSuccess(ENDPOINT_VULCAN_API_PUSH_CONFIG)
        }
    } ?: onSuccess(ENDPOINT_VULCAN_API_PUSH_CONFIG) }
}

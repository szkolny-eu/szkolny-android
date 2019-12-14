/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-20
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.api

import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanApi

class VulcanApiTemplate(override val data: DataVulcan, val onSuccess: () -> Unit) : VulcanApi(data) {
    companion object {
        const val TAG = "VulcanApi"
    }

    init {
        /* data.profile?.also { profile ->
            apiGet(TAG, VULCAN_API_ENDPOINT_) { json, _ ->

                data.setSyncNext(ENDPOINT_VULCAN_API_, SYNC_ALWAYS)
                onSuccess()
            }
        } */
    }
}

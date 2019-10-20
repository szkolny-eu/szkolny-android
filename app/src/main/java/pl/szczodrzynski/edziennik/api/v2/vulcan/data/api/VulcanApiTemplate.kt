/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-20
 */

package pl.szczodrzynski.edziennik.api.v2.vulcan.data.api

import pl.szczodrzynski.edziennik.api.v2.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.api.v2.vulcan.data.VulcanApi
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.getJsonArray

class VulcanApiTemplate(override val data: DataVulcan, val onSuccess: () -> Unit) : VulcanApi(data) {
    companion object {
        const val TAG = "VulcanApi"
    }

    init {
        /* apiGet(TAG, VULCAN_API_ENDPOINT_) { json, _ ->

            data.setSyncNext(ENDPOINT_VULCAN_API_, SYNC_ALWAYS)
            onSuccess()
        } */
    }
}

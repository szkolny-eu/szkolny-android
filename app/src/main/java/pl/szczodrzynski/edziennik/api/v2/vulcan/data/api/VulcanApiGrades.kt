/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-19
 */

package pl.szczodrzynski.edziennik.api.v2.vulcan.data.api

import pl.szczodrzynski.edziennik.api.v2.VULCAN_API_ENDPOINT_GRADES
import pl.szczodrzynski.edziennik.api.v2.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.api.v2.vulcan.ENDPOINT_VULCAN_API_GRADES
import pl.szczodrzynski.edziennik.api.v2.vulcan.data.VulcanApi
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.getJsonArray

class VulcanApiGrades(override val data: DataVulcan, val onSuccess: () -> Unit) : VulcanApi(data) {
    companion object {
        const val TAG = "VulcanApiGrades"
    }

    init {
        apiGet(TAG, VULCAN_API_ENDPOINT_GRADES) { json ->
            val grades = json.getJsonArray("Data")

            data.setSyncNext(ENDPOINT_VULCAN_API_GRADES, SYNC_ALWAYS)
            onSuccess()
        }
    }
}

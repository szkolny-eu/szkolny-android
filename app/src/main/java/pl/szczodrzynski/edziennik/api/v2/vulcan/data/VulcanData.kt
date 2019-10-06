/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-6.
 */

package pl.szczodrzynski.edziennik.api.v2.vulcan.data

import pl.szczodrzynski.edziennik.api.v2.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.utils.Utils

class VulcanData(val data: DataVulcan, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "VulcanData"
    }

    private var cancelled = false

    init {
        nextEndpoint(onSuccess)
    }

    private fun nextEndpoint(onSuccess: () -> Unit) {
        if (data.targetEndpointIds.isEmpty()) {
            onSuccess()
            return
        }
        useEndpoint(data.targetEndpointIds.removeAt(0)) {
            if (cancelled) {
                onSuccess()
                return@useEndpoint
            }
            nextEndpoint(onSuccess)
        }
    }

    private fun useEndpoint(endpointId: Int, onSuccess: () -> Unit) {
        Utils.d(TAG, "Using endpoint $endpointId")
        when (endpointId) {
            /*ENDPOINT_VULCAN_API -> {
                data.startProgress(R.string.edziennik_progress_endpoint_data)
                VulcanApi(data) { onSuccess() }
            }*/
            else -> onSuccess()
        }
    }
}
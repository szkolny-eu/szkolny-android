/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-25. 
 */

package pl.szczodrzynski.edziennik.api.v2.idziennik.data

import pl.szczodrzynski.edziennik.api.v2.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.utils.Utils

class IdziennikData(val data: DataIdziennik, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "IdziennikData"
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
        if (cancelled) {
            onSuccess()
            return
        }
        useEndpoint(data.targetEndpointIds.removeAt(0)) {
            nextEndpoint(onSuccess)
        }
    }

    private fun useEndpoint(endpointId: Int, onSuccess: () -> Unit) {
        Utils.d(TAG, "Using endpoint $endpointId")
        when (endpointId) {
            /*ENDPOINT_IDZIENNIK_WEB_SAMPLE -> {
                data.startProgress(R.string.edziennik_progress_endpoint_student_info)
                IdziennikWebSample(data) { onSuccess() }
            }*/
            else -> onSuccess()
        }
    }
}

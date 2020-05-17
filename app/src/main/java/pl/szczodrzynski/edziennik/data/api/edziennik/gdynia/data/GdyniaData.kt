/*
 * Copyright (c) Kacper Ziubryniewicz 2020-5-17
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.gdynia.data

import pl.szczodrzynski.edziennik.data.api.edziennik.gdynia.DataGdynia
import pl.szczodrzynski.edziennik.utils.Utils

class GdyniaData(val data: DataGdynia, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "GdyniaData"
    }

    init {
        nextEndpoint(onSuccess)
    }

    private fun nextEndpoint(onSuccess: () -> Unit) {
        if (data.targetEndpointIds.isEmpty()) {
            onSuccess()
            return
        }
        if (data.cancelled) {
            onSuccess()
            return
        }
        val id = data.targetEndpointIds.firstKey()
        val lastSync = data.targetEndpointIds.remove(id)
        useEndpoint(id, lastSync) {
            data.progress(data.progressStep)
            nextEndpoint(onSuccess)
        }
    }

    private fun useEndpoint(endpointId: Int, lastSync: Long?, onSuccess: (endpointId: Int) -> Unit) {
        Utils.d(TAG, "Using endpoint $endpointId. Last sync time = $lastSync")
        when (endpointId) {
            else -> onSuccess(endpointId)
        }
    }
}

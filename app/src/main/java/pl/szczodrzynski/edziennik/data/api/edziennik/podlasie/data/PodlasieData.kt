/*
 * Copyright (c) Kacper Ziubryniewicz 2020-5-12
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.data

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.DataPodlasie
import pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.ENDPOINT_PODLASIE_API_MAIN
import pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.data.api.PodlasieApiMain
import pl.szczodrzynski.edziennik.utils.Utils

class PodlasieData(val data: DataPodlasie, val onSuccess: () -> Unit) {
    companion object {
        const val TAG = "PodlasieData"
    }

    init {
        nextEndpoint(onSuccess)
    }

    private fun nextEndpoint(onSuccess: () -> Unit) {
        if (data.targetEndpoints.isEmpty()) {
            onSuccess()
            return
        }
        if (data.cancelled) {
            onSuccess()
            return
        }
        val id = data.targetEndpoints.firstKey()
        val lastSync = data.targetEndpoints.remove(id)
        useEndpoint(id, lastSync) {
            data.progress(data.progressStep)
            nextEndpoint(onSuccess)
        }
    }

    private fun useEndpoint(endpointId: Int, lastSync: Long?, onSuccess: (endpointId: Int) -> Unit) {
        Utils.d(TAG, "Using endpoint $endpointId. Last sync time = $lastSync")
        when (endpointId) {
            ENDPOINT_PODLASIE_API_MAIN -> {
                data.startProgress(R.string.edziennik_progress_endpoint_data)
                PodlasieApiMain(data, lastSync, onSuccess)
            }
            else -> onSuccess(endpointId)
        }
    }
}

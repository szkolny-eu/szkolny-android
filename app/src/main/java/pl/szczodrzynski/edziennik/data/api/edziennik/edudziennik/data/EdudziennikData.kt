/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-22
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data

import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.utils.Utils

class EdudziennikData(val data: DataEdudziennik, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "TemplateData"
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
        useEndpoint(data.targetEndpointIds.removeAt(0)) {
            data.progress(data.progressStep)
            nextEndpoint(onSuccess)
        }
    }

    private fun useEndpoint(endpointId: Int, onSuccess: () -> Unit) {
        Utils.d(TAG, "Using endpoint $endpointId")
        when (endpointId) {
            // TODO
            else -> onSuccess()
        }
    }
}

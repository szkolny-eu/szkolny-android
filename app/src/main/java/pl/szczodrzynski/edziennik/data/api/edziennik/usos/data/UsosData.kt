/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-13.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.usos.data

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.edziennik.template.data.web.TemplateWebSample
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.DataUsos
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.ENDPOINT_USOS_API_USER
import pl.szczodrzynski.edziennik.utils.Utils.d

class UsosData(val data: DataUsos, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "UsosData"
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
        useEndpoint(id, lastSync) { endpointId ->
            data.progress(data.progressStep)
            nextEndpoint(onSuccess)
        }
    }

    private fun useEndpoint(endpointId: Int, lastSync: Long?, onSuccess: (endpointId: Int) -> Unit) {
        d(TAG, "Using endpoint $endpointId. Last sync time = $lastSync")
        when (endpointId) {
            ENDPOINT_USOS_API_USER -> {
                data.startProgress(R.string.edziennik_progress_endpoint_student_info)
//                TemplateWebSample(data, lastSync, onSuccess)
            }
            else -> onSuccess(endpointId)
        }
    }
}

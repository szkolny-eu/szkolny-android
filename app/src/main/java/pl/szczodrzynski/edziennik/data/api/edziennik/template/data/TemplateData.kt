/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.template.data

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.edziennik.template.DataTemplate
import pl.szczodrzynski.edziennik.data.api.edziennik.template.ENDPOINT_TEMPLATE_API_SAMPLE
import pl.szczodrzynski.edziennik.data.api.edziennik.template.ENDPOINT_TEMPLATE_WEB_SAMPLE
import pl.szczodrzynski.edziennik.data.api.edziennik.template.ENDPOINT_TEMPLATE_WEB_SAMPLE_2
import pl.szczodrzynski.edziennik.data.api.edziennik.template.data.api.TemplateApiSample
import pl.szczodrzynski.edziennik.data.api.edziennik.template.data.web.TemplateWebSample
import pl.szczodrzynski.edziennik.data.api.edziennik.template.data.web.TemplateWebSample2
import pl.szczodrzynski.edziennik.utils.Utils
import timber.log.Timber

class TemplateData(val data: DataTemplate, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "TemplateData"
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
        useEndpoint(id, lastSync) { endpointId ->
            data.progress(data.progressStep)
            nextEndpoint(onSuccess)
        }
    }

    private fun useEndpoint(endpointId: Int, lastSync: Long?, onSuccess: (endpointId: Int) -> Unit) {
        Timber.d("Using endpoint $endpointId. Last sync time = $lastSync")
        when (endpointId) {
            ENDPOINT_TEMPLATE_WEB_SAMPLE -> {
                data.startProgress(R.string.edziennik_progress_endpoint_student_info)
                TemplateWebSample(data, lastSync, onSuccess)
            }
            ENDPOINT_TEMPLATE_WEB_SAMPLE_2 -> {
                data.startProgress(R.string.edziennik_progress_endpoint_school_info)
                TemplateWebSample2(data, lastSync, onSuccess)
            }
            ENDPOINT_TEMPLATE_API_SAMPLE -> {
                data.startProgress(R.string.edziennik_progress_endpoint_grades)
                TemplateApiSample(data, lastSync, onSuccess)
            }
            else -> onSuccess(endpointId)
        }
    }
}

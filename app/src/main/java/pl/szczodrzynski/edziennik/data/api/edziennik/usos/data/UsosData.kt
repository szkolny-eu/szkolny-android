/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-13.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.usos.data

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.edziennik.template.data.web.TemplateWebSample
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.*
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.data.api.UsosApiCourses
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.data.api.UsosApiTerms
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.data.api.UsosApiTimetable
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
            /*ENDPOINT_USOS_API_USER -> {
                data.startProgress(R.string.edziennik_progress_endpoint_student_info)
//                TemplateWebSample(data, lastSync, onSuccess)
            }*/
            ENDPOINT_USOS_API_TERMS -> {
                data.startProgress(R.string.edziennik_progress_endpoint_school_info)
                UsosApiTerms(data, lastSync, onSuccess)
            }
            ENDPOINT_USOS_API_COURSES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_teams)
                UsosApiCourses(data, lastSync, onSuccess)
            }
            ENDPOINT_USOS_API_TIMETABLE -> {
                data.startProgress(R.string.edziennik_progress_endpoint_timetable)
                UsosApiTimetable(data, lastSync, onSuccess)
            }
            else -> onSuccess(endpointId)
        }
    }
}

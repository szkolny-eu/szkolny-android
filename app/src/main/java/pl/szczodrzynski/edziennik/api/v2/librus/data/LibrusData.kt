/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.api.v2.librus.*
import pl.szczodrzynski.edziennik.api.v2.librus.data.api.*
import pl.szczodrzynski.edziennik.utils.Utils

class LibrusData(val data: DataLibrus, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "LibrusEndpoints"
    }

    init {
        nextEndpoint(onSuccess)
    }

    private fun nextEndpoint(onSuccess: () -> Unit) {
        if (data.targetEndpointIds.isEmpty()) {
            onSuccess()
            return
        }
        useEndpoint(data.targetEndpointIds.removeAt(0)) {
            if (data.cancelled) {
                onSuccess()
                return@useEndpoint
            }
            nextEndpoint(onSuccess)
        }
    }

    private fun useEndpoint(endpointId: Int, onSuccess: () -> Unit) {
        Utils.d(TAG, "Using endpoint $endpointId")
        when (endpointId) {
            ENDPOINT_LIBRUS_API_ME -> {
                data.startProgress(R.string.sync_action_getting_account)
                LibrusApiMe(data) { onSuccess() }
            }
            ENDPOINT_LIBRUS_API_SCHOOLS -> {
                data.startProgress(R.string.sync_action_syncing_school_info)
                LibrusApiSchools(data) { onSuccess() }
            }
            ENDPOINT_LIBRUS_API_NORMAL_GRADES -> {
                data.startProgress(R.string.sync_action_syncing_grades)
                LibrusApiGrades(data) { onSuccess() }
            }
            ENDPOINT_LIBRUS_API_EVENTS -> {
                data.startProgress(R.string.sync_action_syncing_events)
                LibrusApiEvents(data) { onSuccess() }
            }
            ENDPOINT_LIBRUS_API_HOMEWORK -> {
                data.startProgress(R.string.sync_action_syncing_homework)
                LibrusApiHomework(data) { onSuccess() }
            }
            else -> onSuccess()
        }
    }
}

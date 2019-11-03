/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-6.
 */

package pl.szczodrzynski.edziennik.api.v2.vulcan.data

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.api.v2.vulcan.*
import pl.szczodrzynski.edziennik.api.v2.vulcan.data.api.*
import pl.szczodrzynski.edziennik.utils.Utils

class VulcanData(val data: DataVulcan, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "VulcanData"
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
            ENDPOINT_VULCAN_API_DICTIONARIES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_dictionaries)
                VulcanApiDictionaries(data) { onSuccess() }
            }
            ENDPOINT_VULCAN_API_GRADES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_grades)
                VulcanApiGrades(data) { onSuccess() }
            }
            ENDPOINT_VULCAN_API_GRADES_SUMMARY -> {
                data.startProgress(R.string.edziennik_progress_endpoint_proposed_grades)
                VulcanApiProposedGrades(data) { onSuccess() }
            }
            ENDPOINT_VULCAN_API_EVENTS -> {
                data.startProgress(R.string.edziennik_progress_endpoint_events)
                VulcanApiEvents(data, isHomework = false) { onSuccess() }
            }
            ENDPOINT_VULCAN_API_HOMEWORK -> {
                data.startProgress(R.string.edziennik_progress_endpoint_homework)
                VulcanApiEvents(data, isHomework = true) { onSuccess() }
            }
            ENDPOINT_VULCAN_API_NOTICES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_notices)
                VulcanApiNotices(data) { onSuccess() }
            }
            ENDPOINT_VULCAN_API_ATTENDANCE -> {
                data.startProgress(R.string.edziennik_progress_endpoint_attendance)
                VulcanApiAttendance(data) { onSuccess() }
            }
            ENDPOINT_VULCAN_API_MESSAGES_INBOX -> {
                data.startProgress(R.string.edziennik_progress_endpoint_messages_inbox)
                VulcanApiMessagesInbox(data) { onSuccess() }
            }
            else -> onSuccess()
        }
    }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-6.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.*
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.api.*
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
        useEndpoint(data.targetEndpointIds.firstKey()) { endpointId ->
            data.targetEndpointIds.remove(endpointId)
            data.progress(data.progressStep)
            nextEndpoint(onSuccess)
        }
    }

    private fun useEndpoint(endpointId: Int, onSuccess: (endpointId: Int) -> Unit) {
        val lastSync = data.targetEndpointIds[endpointId]
        Utils.d(TAG, "Using endpoint $endpointId. Last sync time = $lastSync")
        when (endpointId) {
            ENDPOINT_VULCAN_API_UPDATE_SEMESTER -> {
                data.startProgress(R.string.edziennik_progress_endpoint_student_info)
                VulcanApiUpdateSemester(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_API_DICTIONARIES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_dictionaries)
                VulcanApiDictionaries(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_API_GRADES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_grades)
                VulcanApiGrades(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_API_GRADES_SUMMARY -> {
                data.startProgress(R.string.edziennik_progress_endpoint_proposed_grades)
                VulcanApiProposedGrades(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_API_EVENTS -> {
                data.startProgress(R.string.edziennik_progress_endpoint_events)
                VulcanApiEvents(data, isHomework = false, lastSync = lastSync, onSuccess = onSuccess)
            }
            ENDPOINT_VULCAN_API_HOMEWORK -> {
                data.startProgress(R.string.edziennik_progress_endpoint_homework)
                VulcanApiEvents(data, isHomework = true, lastSync = lastSync, onSuccess = onSuccess)
            }
            ENDPOINT_VULCAN_API_NOTICES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_notices)
                VulcanApiNotices(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_API_ATTENDANCE -> {
                data.startProgress(R.string.edziennik_progress_endpoint_attendance)
                VulcanApiAttendance(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_API_TIMETABLE -> {
                data.startProgress(R.string.edziennik_progress_endpoint_timetable)
                VulcanApiTimetable(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_API_MESSAGES_INBOX -> {
                data.startProgress(R.string.edziennik_progress_endpoint_messages_inbox)
                VulcanApiMessagesInbox(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_API_MESSAGES_SENT -> {
                data.startProgress(R.string.edziennik_progress_endpoint_messages_outbox)
                VulcanApiMessagesSent(data, lastSync, onSuccess)
            }
            else -> onSuccess(endpointId)
        }
    }
}

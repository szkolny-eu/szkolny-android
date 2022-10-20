/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.api.MobidziennikApi
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.api2.MobidziennikApi2Main
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.web.*
import pl.szczodrzynski.edziennik.utils.Utils

class MobidziennikData(val data: DataMobidziennik, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "MobidziennikData"
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
        Utils.d(TAG, "Using endpoint $endpointId. Last sync time = $lastSync")
        when (endpointId) {
            ENDPOINT_MOBIDZIENNIK_API_MAIN -> {
                data.startProgress(R.string.edziennik_progress_endpoint_data)
                MobidziennikApi(data, lastSync, onSuccess)
            }
            ENDPOINT_MOBIDZIENNIK_API2_MAIN -> {
                data.startProgress(R.string.edziennik_progress_endpoint_push_config)
                MobidziennikApi2Main(data, lastSync, onSuccess)
            }
            ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_INBOX -> {
                data.startProgress(R.string.edziennik_progress_endpoint_messages_inbox)
                MobidziennikWebMessagesInbox(data, lastSync, onSuccess)
            }
            ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_SENT -> {
                data.startProgress(R.string.edziennik_progress_endpoint_messages_outbox)
                MobidziennikWebMessagesSent(data, lastSync, onSuccess)
            }
            ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_ALL -> {
                data.startProgress(R.string.edziennik_progress_endpoint_messages)
                MobidziennikWebMessagesAll(data, lastSync, onSuccess)
            }
            ENDPOINT_MOBIDZIENNIK_WEB_CALENDAR -> {
                data.startProgress(R.string.edziennik_progress_endpoint_calendar)
                MobidziennikWebCalendar(data, lastSync, onSuccess)
            }
            ENDPOINT_MOBIDZIENNIK_WEB_GRADES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_grades)
                MobidziennikWebGrades(data, lastSync, onSuccess)
            }
            ENDPOINT_MOBIDZIENNIK_WEB_ACCOUNT_EMAIL -> {
                data.startProgress(R.string.edziennik_progress_endpoint_account_details)
                MobidziennikWebAccountEmail(data, lastSync, onSuccess)
            }
            ENDPOINT_MOBIDZIENNIK_WEB_ATTENDANCE -> {
                data.startProgress(R.string.edziennik_progress_endpoint_attendance)
                MobidziennikWebAttendance(data, lastSync, onSuccess)
            }/*
            ENDPOINT_MOBIDZIENNIK_WEB_NOTICES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_behaviour)
                MobidziennikWebNotices(data, lastSync, onSuccess)
            }]
            ENDPOINT_MOBIDZIENNIK_WEB_MANUALS -> {
                data.startProgress(R.string.edziennik_progress_endpoint_lucky_number)
                MobidziennikWebManuals(data, lastSync, onSuccess)
            }*/
            ENDPOINT_MOBIDZIENNIK_WEB_TIMETABLE-> {
                data.startProgress(R.string.edziennik_progress_endpoint_timetable)
                MobidziennikWebTimetable(data, lastSync, onSuccess)
            }
            else -> onSuccess(endpointId)
        }
    }
}

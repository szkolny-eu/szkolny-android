/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.api.v2.mobidziennik.data

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.*
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.data.api.MobidziennikApi
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.data.web.MobidziennikWebCalendar
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.data.web.MobidziennikWebGrades
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.data.web.MobidziennikWebMessagesAll
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.data.web.MobidziennikWebMessagesInbox
import pl.szczodrzynski.edziennik.utils.Utils

class MobidziennikData(val data: DataMobidziennik, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "MobidziennikData"
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
            ENDPOINT_MOBIDZIENNIK_API_MAIN -> {
                data.startProgress(R.string.edziennik_progress_endpoint_data)
                MobidziennikApi(data) { onSuccess() }
            }
            ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_INBOX -> {
                data.startProgress(R.string.edziennik_progress_endpoint_messages_inbox)
                MobidziennikWebMessagesInbox(data) { onSuccess() }
            }
            ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_ALL -> {
                data.startProgress(R.string.edziennik_progress_endpoint_messages)
                MobidziennikWebMessagesAll(data) { onSuccess() }
            }
            ENDPOINT_MOBIDZIENNIK_WEB_CALENDAR -> {
                data.startProgress(R.string.edziennik_progress_endpoint_calendar)
                MobidziennikWebCalendar(data) { onSuccess() }
            }
            ENDPOINT_MOBIDZIENNIK_WEB_GRADES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_grades)
                MobidziennikWebGrades(data) { onSuccess() }
            }/*
            ENDPOINT_MOBIDZIENNIK_WEB_NOTICES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_behaviour)
                MobidziennikWebNotices(data) { onSuccess() }
            }
            ENDPOINT_MOBIDZIENNIK_WEB_ATTENDANCE -> {
                data.startProgress(R.string.edziennik_progress_endpoint_attendance)
                MobidziennikWebAttendance(data) { onSuccess() }
            }
            ENDPOINT_MOBIDZIENNIK_WEB_MANUALS -> {
                data.startProgress(R.string.edziennik_progress_endpoint_lucky_number)
                MobidziennikWebManuals(data) { onSuccess() }
            }*/
            else -> onSuccess()
        }
    }
}
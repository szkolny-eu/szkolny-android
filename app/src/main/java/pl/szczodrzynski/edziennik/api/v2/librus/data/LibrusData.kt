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
                data.startProgress(R.string.edziennik_progress_endpoint_student_info)
                LibrusApiMe(data) { onSuccess() }
            }
            ENDPOINT_LIBRUS_API_SCHOOLS -> {
                data.startProgress(R.string.edziennik_progress_endpoint_school_info)
                LibrusApiSchools(data) { onSuccess() }
            }
            ENDPOINT_LIBRUS_API_NORMAL_GRADES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_grades)
                LibrusApiGrades(data) { onSuccess() }
            }
            ENDPOINT_LIBRUS_API_EVENTS -> {
                data.startProgress(R.string.edziennik_progress_endpoint_events)
                LibrusApiEvents(data) { onSuccess() }
            }
            ENDPOINT_LIBRUS_API_HOMEWORK -> {
                data.startProgress(R.string.edziennik_progress_endpoint_homework)
                LibrusApiHomework(data) { onSuccess() }
            }
            ENDPOINT_LIBRUS_API_ATTENDANCE_TYPES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_attendance_types)
                LibrusApiAttendanceTypes(data) { onSuccess() }
            }
            ENDPOINT_LIBRUS_API_ATTENDANCES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_attendance)
                LibrusApiAttendances(data) { onSuccess() }
            }
            ENDPOINT_LIBRUS_API_ANNOUNCEMENTS -> {
                data.startProgress(R.string.edziennik_progress_endpoint_announcements)
                LibrusApiAnnouncements(data) { onSuccess() }
            }
            else -> onSuccess()
        }
    }
}

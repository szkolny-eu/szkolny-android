/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-6.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.ERROR_VULCAN_API_DEPRECATED
import pl.szczodrzynski.edziennik.data.api.LOGIN_MODE_VULCAN_API
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.*
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe.*
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.web.VulcanWebLuckyNumber
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.utils.Utils

class VulcanData(val data: DataVulcan, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "VulcanData"
    }

    private var firstSemesterSync = false
    private val firstSemesterSyncExclude = listOf(
        ENDPOINT_VULCAN_HEBE_MAIN,
        ENDPOINT_VULCAN_HEBE_ADDRESSBOOK,
        ENDPOINT_VULCAN_HEBE_TIMETABLE,
        ENDPOINT_VULCAN_HEBE_MESSAGES_INBOX,
        ENDPOINT_VULCAN_HEBE_MESSAGES_SENT
    )

    init { run {
        if (data.loginStore.mode == LOGIN_MODE_VULCAN_API) {
            data.error(TAG, ERROR_VULCAN_API_DEPRECATED)
            return@run
        }
        if (data.studentSemesterNumber == 2 && data.profile?.empty != false) {
            firstSemesterSync = true
            // set to sync 1st semester first
            data.studentSemesterId = data.semester1Id
            data.studentSemesterNumber = 1
        }
        nextEndpoint {
            if (firstSemesterSync) {
                // at the end, set back 2nd semester
                data.studentSemesterId = data.semester2Id
                data.studentSemesterNumber = 2
            }
            onSuccess()
        }
    }}

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
        useEndpoint(id, lastSync) {
            if (firstSemesterSync && id !in firstSemesterSyncExclude) {
                // sync 2nd semester after every endpoint
                data.studentSemesterId = data.semester2Id
                data.studentSemesterNumber = 2
                useEndpoint(id, lastSync) {
                    // set 1st semester back for the next endpoint
                    data.studentSemesterId = data.semester1Id
                    data.studentSemesterNumber = 1
                    // progress further
                    data.progress(data.progressStep)
                    nextEndpoint(onSuccess)
                }
                return@useEndpoint
            }
            data.progress(data.progressStep)
            nextEndpoint(onSuccess)
        }
    }

    private fun useEndpoint(endpointId: Int, lastSync: Long?, onSuccess: (endpointId: Int) -> Unit) {
        Utils.d(TAG, "Using endpoint $endpointId. Last sync time = $lastSync")
        when (endpointId) {
            ENDPOINT_VULCAN_API_UPDATE_SEMESTER -> {
                data.startProgress(R.string.edziennik_progress_endpoint_student_info)
                VulcanApiUpdateSemester(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_API_PUSH_CONFIG -> {
                data.startProgress(R.string.edziennik_progress_endpoint_push_config)
                VulcanApiPushConfig(data, lastSync, onSuccess)
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
            ENDPOINT_VULCAN_WEB_LUCKY_NUMBERS -> {
                data.startProgress(R.string.edziennik_progress_endpoint_lucky_number)
                VulcanWebLuckyNumber(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_HEBE_MAIN -> {
                if (data.profile == null) {
                    onSuccess(ENDPOINT_VULCAN_HEBE_MAIN)
                    return
                }
                data.startProgress(R.string.edziennik_progress_endpoint_student_info)
                VulcanHebeMain(data, lastSync).getStudents(
                    profile = data.profile,
                    profileList = null
                ) {
                    onSuccess(ENDPOINT_VULCAN_HEBE_MAIN)
                }
            }
            ENDPOINT_VULCAN_HEBE_PUSH_CONFIG -> {
                data.startProgress(R.string.edziennik_progress_endpoint_push_config)
                VulcanHebePushConfig(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_HEBE_ADDRESSBOOK -> {
                data.startProgress(R.string.edziennik_progress_endpoint_teachers)
                VulcanHebeAddressbook(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_HEBE_TIMETABLE -> {
                data.startProgress(R.string.edziennik_progress_endpoint_timetable)
                VulcanHebeTimetable(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_HEBE_EXAMS -> {
                data.startProgress(R.string.edziennik_progress_endpoint_exams)
                VulcanHebeExams(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_HEBE_GRADES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_grades)
                VulcanHebeGrades(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_HEBE_HOMEWORK -> {
                data.startProgress(R.string.edziennik_progress_endpoint_homework)
                VulcanHebeHomework(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_HEBE_MESSAGES_INBOX -> {
                data.startProgress(R.string.edziennik_progress_endpoint_messages_inbox)
                VulcanHebeMessages(data, lastSync, onSuccess).getMessages(Message.TYPE_RECEIVED)
            }
            ENDPOINT_VULCAN_HEBE_MESSAGES_SENT -> {
                data.startProgress(R.string.edziennik_progress_endpoint_messages_outbox)
                VulcanHebeMessages(data, lastSync, onSuccess).getMessages(Message.TYPE_SENT)
            }
            ENDPOINT_VULCAN_HEBE_ATTENDANCE -> {
                data.startProgress(R.string.edziennik_progress_endpoint_attendance)
                VulcanHebeAttendance(data, lastSync, onSuccess)
            }
            ENDPOINT_VULCAN_HEBE_LUCKY_NUMBER -> {
                data.startProgress(R.string.edziennik_progress_endpoint_lucky_number)
                VulcanHebeLuckyNumber(data, lastSync, onSuccess)
            }
            else -> onSuccess(endpointId)
        }
    }
}

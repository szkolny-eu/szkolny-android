/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.messages.LibrusMessagesGetList
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.synergia.LibrusSynergiaGetMessages
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.synergia.LibrusSynergiaHomework
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.synergia.LibrusSynergiaInfo
import pl.szczodrzynski.edziennik.data.db.entity.Message
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
        Utils.d(TAG, "Using endpoint $endpointId. Last sync time = $lastSync")
        when (endpointId) {
            /**
             * API
             */
            ENDPOINT_LIBRUS_API_ME -> {
                data.startProgress(R.string.edziennik_progress_endpoint_student_info)
                LibrusApiMe(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_SCHOOLS -> {
                data.startProgress(R.string.edziennik_progress_endpoint_school_info)
                LibrusApiSchools(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_CLASSES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_classes)
                LibrusApiClasses(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_VIRTUAL_CLASSES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_teams)
                LibrusApiVirtualClasses(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_UNITS -> {
                data.startProgress(R.string.edziennik_progress_endpoint_units)
                LibrusApiUnits(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_USERS -> {
                data.startProgress(R.string.edziennik_progress_endpoint_teachers)
                LibrusApiUsers(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_SUBJECTS -> {
                data.startProgress(R.string.edziennik_progress_endpoint_subjects)
                LibrusApiSubjects(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_CLASSROOMS -> {
                data.startProgress(R.string.edziennik_progress_endpoint_classrooms)
                LibrusApiClassrooms(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_LESSONS -> {
                data.startProgress(R.string.edziennik_progress_endpoint_lessons)
                LibrusApiLessons(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_PUSH_CONFIG -> {
                data.startProgress(R.string.edziennik_progress_endpoint_push_config)
                LibrusApiPushConfig(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_TIMETABLES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_timetable)
                LibrusApiTimetables(data, lastSync, onSuccess)
            }

            ENDPOINT_LIBRUS_API_NORMAL_GRADE_CATEGORIES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_grade_categories)
                LibrusApiGradeCategories(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADE_CATEGORIES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_grade_categories)
                LibrusApiBehaviourGradeCategories(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_DESCRIPTIVE_GRADE_CATEGORIES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_grade_categories)
                LibrusApiDescriptiveGradeCategories(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_TEXT_GRADE_CATEGORIES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_grade_categories)
                LibrusApiTextGradeCategories(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_POINT_GRADE_CATEGORIES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_grade_categories)
                LibrusApiPointGradeCategories(data, lastSync, onSuccess)
            }

            ENDPOINT_LIBRUS_API_NORMAL_GRADE_COMMENTS -> {
                data.startProgress(R.string.edziennik_progress_endpoint_grade_comments)
                LibrusApiGradeComments(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADE_COMMENTS -> {
                data.startProgress(R.string.edziennik_progress_endpoint_grade_comments)
                LibrusApiBehaviourGradeComments(data, lastSync, onSuccess)
            }

            ENDPOINT_LIBRUS_API_NORMAL_GRADES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_grades)
                LibrusApiGrades(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_behaviour_grades)
                LibrusApiBehaviourGrades(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_DESCRIPTIVE_GRADES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_descriptive_grades)
                LibrusApiDescriptiveGrades(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_TEXT_GRADES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_descriptive_grades)
                LibrusApiTextGrades(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_POINT_GRADES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_point_grades)
                LibrusApiPointGrades(data, lastSync, onSuccess)
            }

            ENDPOINT_LIBRUS_API_EVENT_TYPES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_event_types)
                LibrusApiEventTypes(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_EVENTS -> {
                data.startProgress(R.string.edziennik_progress_endpoint_events)
                LibrusApiEvents(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_HOMEWORK -> {
                data.startProgress(R.string.edziennik_progress_endpoint_homework)
                LibrusApiHomework(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_LUCKY_NUMBER -> {
                data.startProgress(R.string.edziennik_progress_endpoint_lucky_number)
                LibrusApiLuckyNumber(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_NOTICE_TYPES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_notice_types)
                LibrusApiNoticeTypes(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_NOTICES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_notices)
                LibrusApiNotices(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_ATTENDANCE_TYPES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_attendance_types)
                LibrusApiAttendanceTypes(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_ATTENDANCES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_attendance)
                LibrusApiAttendances(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_ANNOUNCEMENTS -> {
                data.startProgress(R.string.edziennik_progress_endpoint_announcements)
                LibrusApiAnnouncements(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_PT_MEETINGS -> {
                data.startProgress(R.string.edziennik_progress_endpoint_pt_meetings)
                LibrusApiPtMeetings(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_TEACHER_FREE_DAY_TYPES -> {
                data.startProgress(R.string.edziennik_progress_endpoint_teacher_free_day_types)
                LibrusApiTeacherFreeDayTypes(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_API_TEACHER_FREE_DAYS -> {
                data.startProgress(R.string.edziennik_progress_endpoint_teacher_free_days)
                LibrusApiTeacherFreeDays(data, lastSync, onSuccess)
            }

            /**
             * SYNERGIA
             */
            ENDPOINT_LIBRUS_SYNERGIA_HOMEWORK -> {
                data.startProgress(R.string.edziennik_progress_endpoint_homework)
                LibrusSynergiaHomework(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_SYNERGIA_INFO -> {
                data.startProgress(R.string.edziennik_progress_endpoint_student_info)
                LibrusSynergiaInfo(data, lastSync, onSuccess)
            }
            ENDPOINT_LIBRUS_SYNERGIA_MESSAGES_RECEIVED -> {
                data.startProgress(R.string.edziennik_progress_endpoint_messages_inbox)
                LibrusSynergiaGetMessages(data, type = Message.TYPE_RECEIVED, lastSync = lastSync, onSuccess = onSuccess)
            }
            ENDPOINT_LIBRUS_SYNERGIA_MESSAGES_SENT -> {
                data.startProgress(R.string.edziennik_progress_endpoint_messages_outbox)
                LibrusSynergiaGetMessages(data, type = Message.TYPE_SENT, lastSync = lastSync, onSuccess = onSuccess)
            }

            /**
             * MESSAGES
             */
            ENDPOINT_LIBRUS_MESSAGES_RECEIVED -> {
                data.startProgress(R.string.edziennik_progress_endpoint_messages_inbox)
                if (data.messagesLoginSuccessful) LibrusMessagesGetList(data, type = Message.TYPE_RECEIVED, lastSync = lastSync, onSuccess = onSuccess)
                else LibrusSynergiaGetMessages(data, type = Message.TYPE_RECEIVED, lastSync = lastSync, onSuccess = onSuccess)
            }
            ENDPOINT_LIBRUS_MESSAGES_SENT -> {
                data.startProgress(R.string.edziennik_progress_endpoint_messages_outbox)
                if (data.messagesLoginSuccessful) LibrusMessagesGetList(data, type = Message.TYPE_SENT, lastSync = lastSync, onSuccess = onSuccess)
                else LibrusSynergiaGetMessages(data, type = Message.TYPE_SENT, lastSync = lastSync, onSuccess = onSuccess)
            }

            else -> onSuccess(endpointId)
        }
    }
}

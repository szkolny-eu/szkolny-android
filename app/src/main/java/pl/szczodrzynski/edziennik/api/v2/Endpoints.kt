/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-20.
 */

package pl.szczodrzynski.edziennik.api.v2

import pl.szczodrzynski.edziennik.api.v2.models.Endpoint

const val ENDPOINT_LIBRUS_API_ME                        = 101
const val ENDPOINT_LIBRUS_API_SCHOOLS                   = 102
const val ENDPOINT_LIBRUS_API_CLASSES                   = 103
const val ENDPOINT_LIBRUS_API_VIRTUAL_CLASSES           = 104
const val ENDPOINT_LIBRUS_API_UNITS                     = 105
const val ENDPOINT_LIBRUS_API_USERS                     = 106
const val ENDPOINT_LIBRUS_API_SUBJECTS                  = 107
const val ENDPOINT_LIBRUS_API_CLASSROOMS                = 108
const val ENDPOINT_LIBRUS_API_TIMETABLES                = 109
const val ENDPOINT_LIBRUS_API_SUBSTITUTIONS             = 110
const val ENDPOINT_LIBRUS_API_NORMAL_GC                 = 111
const val ENDPOINT_LIBRUS_API_POINT_GC                  = 112
const val ENDPOINT_LIBRUS_API_DESCRIPTIVE_GC            = 113
const val ENDPOINT_LIBRUS_API_TEXT_GC                   = 114
const val ENDPOINT_LIBRUS_API_DESCRIPTIVE_TEXT_GC       = 115
const val ENDPOINT_LIBRUS_API_BEHAVIOUR_GC              = 116
const val ENDPOINT_LIBRUS_API_NORMAL_GRADES             = 117
const val ENDPOINT_LIBRUS_API_POINT_GRADES              = 118
const val ENDPOINT_LIBRUS_API_DESCRIPTIVE_GRADES        = 119
const val ENDPOINT_LIBRUS_API_TEXT_GRADES               = 120
const val ENDPOINT_LIBRUS_API_DESCRIPTIVE_TEXT_GRADES   = 121
const val ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADES          = 122
const val ENDPOINT_LIBRUS_API_EVENTS                    = 123
const val ENDPOINT_LIBRUS_API_EVENT_TYPES               = 124
const val ENDPOINT_LIBRUS_API_HOMEWORK                  = 125
const val ENDPOINT_LIBRUS_API_LUCKY_NUMBER              = 126
const val ENDPOINT_LIBRUS_API_NOTICES                   = 127
const val ENDPOINT_LIBRUS_API_ATTENDANCE_TYPES          = 128
const val ENDPOINT_LIBRUS_API_ATTENDANCE                = 129
const val ENDPOINT_LIBRUS_API_ANNOUNCEMENTS             = 130
const val ENDPOINT_LIBRUS_API_PT_MEETINGS               = 131
const val ENDPOINT_LIBRUS_API_TEACHER_FREE_DAYS         = 132
const val ENDPOINT_LIBRUS_API_SCHOOL_FREE_DAYS          = 133
const val ENDPOINT_LIBRUS_API_CLASS_FREE_DAYS           = 134
const val ENDPOINT_LIBRUS_SYNERGIA_INFO                 = 201
const val ENDPOINT_LIBRUS_SYNERGIA_GRADES               = 202
const val ENDPOINT_LIBRUS_MESSAGES_RECEIVED             = 301
const val ENDPOINT_LIBRUS_MESSAGES_SENT                 = 302
const val ENDPOINT_LIBRUS_MESSAGES_TRASH                = 303
const val ENDPOINT_LIBRUS_MESSAGES_RECEIVERS            = 304
const val ENDPOINT_LIBRUS_MESSAGES_GET                  = 304

val endpoints = listOf(

        // LIBRUS: API
        Endpoint(LOGIN_TYPE_LIBRUS, FEATURE_TIMETABLE, listOf(
                ENDPOINT_LIBRUS_API_TIMETABLES,
                ENDPOINT_LIBRUS_API_SUBSTITUTIONS
        ), listOf(LOGIN_METHOD_LIBRUS_API)),
        Endpoint(LOGIN_TYPE_LIBRUS, FEATURE_AGENDA, listOf(
                ENDPOINT_LIBRUS_API_EVENTS,
                ENDPOINT_LIBRUS_API_EVENT_TYPES,
                ENDPOINT_LIBRUS_API_PT_MEETINGS,
                ENDPOINT_LIBRUS_API_TEACHER_FREE_DAYS,
                ENDPOINT_LIBRUS_API_SCHOOL_FREE_DAYS,
                ENDPOINT_LIBRUS_API_CLASS_FREE_DAYS
        ), listOf(LOGIN_METHOD_LIBRUS_API)),
        Endpoint(LOGIN_TYPE_LIBRUS, FEATURE_GRADES, listOf(
                ENDPOINT_LIBRUS_API_NORMAL_GC,
                ENDPOINT_LIBRUS_API_POINT_GC,
                ENDPOINT_LIBRUS_API_DESCRIPTIVE_GC,
                ENDPOINT_LIBRUS_API_TEXT_GC,
                ENDPOINT_LIBRUS_API_DESCRIPTIVE_TEXT_GC,
                ENDPOINT_LIBRUS_API_BEHAVIOUR_GC,
                ENDPOINT_LIBRUS_API_NORMAL_GRADES,
                ENDPOINT_LIBRUS_API_POINT_GRADES,
                ENDPOINT_LIBRUS_API_DESCRIPTIVE_GRADES,
                ENDPOINT_LIBRUS_API_TEXT_GRADES,
                ENDPOINT_LIBRUS_API_DESCRIPTIVE_TEXT_GRADES,
                ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADES
        ), listOf(LOGIN_METHOD_LIBRUS_API)),
        Endpoint(LOGIN_TYPE_LIBRUS, FEATURE_HOMEWORK, listOf(
                ENDPOINT_LIBRUS_API_HOMEWORK
        ), listOf(LOGIN_METHOD_LIBRUS_API)),
        Endpoint(LOGIN_TYPE_LIBRUS, FEATURE_NOTICES, listOf(
                ENDPOINT_LIBRUS_API_NOTICES
        ), listOf(LOGIN_METHOD_LIBRUS_API)),
        Endpoint(LOGIN_TYPE_LIBRUS, FEATURE_ATTENDANCES, listOf(
                ENDPOINT_LIBRUS_API_ATTENDANCE,
                ENDPOINT_LIBRUS_API_ATTENDANCE_TYPES
        ), listOf(LOGIN_METHOD_LIBRUS_API)),
        Endpoint(LOGIN_TYPE_LIBRUS, FEATURE_ANNOUNCEMENTS, listOf(
                ENDPOINT_LIBRUS_API_ANNOUNCEMENTS
        ), listOf(LOGIN_METHOD_LIBRUS_API)),
        Endpoint(LOGIN_TYPE_LIBRUS, FEATURE_STUDENT_INFO, listOf(
                ENDPOINT_LIBRUS_API_ME
        ), listOf(LOGIN_METHOD_LIBRUS_API)),
        Endpoint(LOGIN_TYPE_LIBRUS, FEATURE_SCHOOL_INFO, listOf(
                ENDPOINT_LIBRUS_API_SCHOOLS,
                ENDPOINT_LIBRUS_API_UNITS
        ), listOf(LOGIN_METHOD_LIBRUS_API)),
        Endpoint(LOGIN_TYPE_LIBRUS, FEATURE_CLASS_INFO, listOf(
                ENDPOINT_LIBRUS_API_CLASSES
        ), listOf(LOGIN_METHOD_LIBRUS_API)),
        Endpoint(LOGIN_TYPE_LIBRUS, FEATURE_TEAM_INFO, listOf(
                ENDPOINT_LIBRUS_API_VIRTUAL_CLASSES
        ), listOf(LOGIN_METHOD_LIBRUS_API)),
        Endpoint(LOGIN_TYPE_LIBRUS, FEATURE_LUCKY_NUMBER, listOf(
                ENDPOINT_LIBRUS_API_LUCKY_NUMBER
        ), listOf(LOGIN_METHOD_LIBRUS_API)),
        Endpoint(LOGIN_TYPE_LIBRUS, FEATURE_TEACHERS, listOf(
                ENDPOINT_LIBRUS_API_USERS
        ), listOf(LOGIN_METHOD_LIBRUS_API)),
        Endpoint(LOGIN_TYPE_LIBRUS, FEATURE_SUBJECTS, listOf(
                ENDPOINT_LIBRUS_API_SUBJECTS
        ), listOf(LOGIN_METHOD_LIBRUS_API)),
        Endpoint(LOGIN_TYPE_LIBRUS, FEATURE_CLASSROOMS, listOf(
                ENDPOINT_LIBRUS_API_CLASSROOMS
        ), listOf(LOGIN_METHOD_LIBRUS_API)),

        Endpoint(LOGIN_TYPE_LIBRUS, FEATURE_STUDENT_INFO, listOf(
                ENDPOINT_LIBRUS_SYNERGIA_INFO
        ), listOf(LOGIN_METHOD_LIBRUS_SYNERGIA)),
        Endpoint(LOGIN_TYPE_LIBRUS, FEATURE_STUDENT_NUMBER, listOf(
                ENDPOINT_LIBRUS_SYNERGIA_INFO
        ), listOf(LOGIN_METHOD_LIBRUS_SYNERGIA)),
        /*Endpoint(LOGIN_TYPE_LIBRUS, FEATURE_GRADES, listOf(
                ENDPOINT_LIBRUS_SYNERGIA_GRADES
        ), listOf(LOGIN_METHOD_LIBRUS_SYNERGIA)),*/


        Endpoint(LOGIN_TYPE_LIBRUS, FEATURE_GRADES, listOf(
                ENDPOINT_LIBRUS_API_NORMAL_GC,
                ENDPOINT_LIBRUS_API_NORMAL_GRADES,
                ENDPOINT_LIBRUS_SYNERGIA_GRADES
        ), listOf(LOGIN_METHOD_LIBRUS_API, LOGIN_METHOD_LIBRUS_SYNERGIA)),

        Endpoint(LOGIN_TYPE_LIBRUS, FEATURE_MESSAGES_INBOX, listOf(ENDPOINT_LIBRUS_MESSAGES_RECEIVED), listOf(LOGIN_METHOD_LIBRUS_MESSAGES)),
        Endpoint(LOGIN_TYPE_LIBRUS, FEATURE_MESSAGES_OUTBOX, listOf(ENDPOINT_LIBRUS_MESSAGES_SENT), listOf(LOGIN_METHOD_LIBRUS_MESSAGES))
)

/*
    SYNC:

    look up all endpoints for the given API and given features

    load "next sync timers" for every endpoint

    exclude every endpoint which does not need to sync now

    check all needed login methods
        create a login method list, using methods' dependencies as well
        use all login methods, saving completed logins to data store

    instantiate all endpoint classes and sync them (writing to data store, returns onSuccess or error Callback)

 */

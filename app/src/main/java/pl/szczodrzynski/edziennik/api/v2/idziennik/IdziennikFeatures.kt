/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-25. 
 */

package pl.szczodrzynski.edziennik.api.v2.idziennik

import pl.szczodrzynski.edziennik.api.v2.*
import pl.szczodrzynski.edziennik.api.v2.models.Feature

const val ENDPOINT_IDZIENNIK_WEB_TIMETABLE              = 1030
const val ENDPOINT_IDZIENNIK_WEB_GRADES                 = 1040
const val ENDPOINT_IDZIENNIK_WEB_PROPOSED_GRADES        = 1050
const val ENDPOINT_IDZIENNIK_WEB_EXAMS                  = 1060
const val ENDPOINT_IDZIENNIK_WEB_NOTICES                = 1070
const val ENDPOINT_IDZIENNIK_WEB_ANNOUNCEMENTS          = 1080
const val ENDPOINT_IDZIENNIK_WEB_ATTENDANCE             = 1090
const val ENDPOINT_IDZIENNIK_WEB_MESSAGES_INBOX         = 1110
const val ENDPOINT_IDZIENNIK_WEB_MESSAGES_SENT          = 1120
const val ENDPOINT_IDZIENNIK_API_CURRENT_REGISTER       = 2010
const val ENDPOINT_IDZIENNIK_API_MESSAGES_INBOX         = 2110
const val ENDPOINT_IDZIENNIK_API_MESSAGES_SENT          = 2120

val IdziennikFeatures = listOf(
        Feature(LOGIN_TYPE_IDZIENNIK, FEATURE_TIMETABLE, listOf(
                ENDPOINT_IDZIENNIK_WEB_TIMETABLE to LOGIN_METHOD_IDZIENNIK_WEB
        ), listOf(LOGIN_METHOD_IDZIENNIK_WEB)),

        Feature(LOGIN_TYPE_IDZIENNIK, FEATURE_GRADES, listOf(
                ENDPOINT_IDZIENNIK_WEB_GRADES to LOGIN_METHOD_IDZIENNIK_WEB,
                ENDPOINT_IDZIENNIK_WEB_PROPOSED_GRADES to LOGIN_METHOD_IDZIENNIK_WEB
        ), listOf(LOGIN_METHOD_IDZIENNIK_WEB)),

        Feature(LOGIN_TYPE_IDZIENNIK, FEATURE_AGENDA, listOf(
                ENDPOINT_IDZIENNIK_WEB_EXAMS to LOGIN_METHOD_IDZIENNIK_WEB
        ), listOf(LOGIN_METHOD_IDZIENNIK_WEB)),

        Feature(LOGIN_TYPE_IDZIENNIK, FEATURE_BEHAVIOUR, listOf(
                ENDPOINT_IDZIENNIK_WEB_NOTICES to LOGIN_METHOD_IDZIENNIK_WEB
        ), listOf(LOGIN_METHOD_IDZIENNIK_WEB)),

        Feature(LOGIN_TYPE_IDZIENNIK, FEATURE_ATTENDANCE, listOf(
                ENDPOINT_IDZIENNIK_WEB_ATTENDANCE to LOGIN_METHOD_IDZIENNIK_WEB
        ), listOf(LOGIN_METHOD_IDZIENNIK_WEB)),

        Feature(LOGIN_TYPE_IDZIENNIK, FEATURE_ANNOUNCEMENTS, listOf(
                ENDPOINT_IDZIENNIK_WEB_ANNOUNCEMENTS to LOGIN_METHOD_IDZIENNIK_WEB
        ), listOf(LOGIN_METHOD_IDZIENNIK_WEB)),

        /*Feature(LOGIN_TYPE_IDZIENNIK, FEATURE_MESSAGES_INBOX, listOf(
                ENDPOINT_IDZIENNIK_WEB_MESSAGES_INBOX to LOGIN_METHOD_IDZIENNIK_WEB
        ), listOf(LOGIN_METHOD_IDZIENNIK_WEB)).withPriority(2),
        Feature(LOGIN_TYPE_IDZIENNIK, FEATURE_MESSAGES_SENT, listOf(
                ENDPOINT_IDZIENNIK_WEB_MESSAGES_SENT to LOGIN_METHOD_IDZIENNIK_WEB
        ), listOf(LOGIN_METHOD_IDZIENNIK_WEB)).withPriority(2),*/

        Feature(LOGIN_TYPE_IDZIENNIK, FEATURE_MESSAGES_INBOX, listOf(
                ENDPOINT_IDZIENNIK_API_MESSAGES_INBOX to LOGIN_METHOD_IDZIENNIK_API
        ), listOf(LOGIN_METHOD_IDZIENNIK_API)),
        Feature(LOGIN_TYPE_IDZIENNIK, FEATURE_MESSAGES_SENT, listOf(
                ENDPOINT_IDZIENNIK_API_MESSAGES_SENT to LOGIN_METHOD_IDZIENNIK_API
        ), listOf(LOGIN_METHOD_IDZIENNIK_API)),

        Feature(LOGIN_TYPE_IDZIENNIK, FEATURE_LUCKY_NUMBER, listOf(
                ENDPOINT_IDZIENNIK_API_CURRENT_REGISTER to LOGIN_METHOD_IDZIENNIK_API
        ), listOf(LOGIN_METHOD_IDZIENNIK_API))
)

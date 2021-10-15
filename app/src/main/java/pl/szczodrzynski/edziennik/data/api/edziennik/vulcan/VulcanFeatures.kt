/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-6. 
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan

import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.models.Feature

const val ENDPOINT_VULCAN_WEB_LUCKY_NUMBERS       = 2010

const val ENDPOINT_VULCAN_HEBE_MAIN               = 3000
const val ENDPOINT_VULCAN_HEBE_PUSH_CONFIG        = 3005
const val ENDPOINT_VULCAN_HEBE_ADDRESSBOOK        = 3010
const val ENDPOINT_VULCAN_HEBE_TIMETABLE          = 3020
const val ENDPOINT_VULCAN_HEBE_EXAMS              = 3030
const val ENDPOINT_VULCAN_HEBE_GRADES             = 3040
const val ENDPOINT_VULCAN_HEBE_GRADE_SUMMARY      = 3050
const val ENDPOINT_VULCAN_HEBE_HOMEWORK           = 3060
const val ENDPOINT_VULCAN_HEBE_NOTICES            = 3070
const val ENDPOINT_VULCAN_HEBE_ATTENDANCE         = 3080
const val ENDPOINT_VULCAN_HEBE_MESSAGES_INBOX     = 3090
const val ENDPOINT_VULCAN_HEBE_MESSAGES_SENT      = 3100
const val ENDPOINT_VULCAN_HEBE_TEACHERS           = 3110
const val ENDPOINT_VULCAN_HEBE_LUCKY_NUMBER       = 3200

val VulcanFeatures = listOf(
        // timetable
        Feature(LOGIN_TYPE_VULCAN, FEATURE_TIMETABLE, listOf(
                ENDPOINT_VULCAN_HEBE_TIMETABLE to LOGIN_METHOD_VULCAN_HEBE
        ), listOf(LOGIN_METHOD_VULCAN_HEBE)),
        // agenda
        Feature(LOGIN_TYPE_VULCAN, FEATURE_AGENDA, listOf(
                ENDPOINT_VULCAN_HEBE_EXAMS to LOGIN_METHOD_VULCAN_HEBE
        ), listOf(LOGIN_METHOD_VULCAN_HEBE)),
        // grades
        Feature(LOGIN_TYPE_VULCAN, FEATURE_GRADES, listOf(
                ENDPOINT_VULCAN_HEBE_GRADES to LOGIN_METHOD_VULCAN_HEBE,
                ENDPOINT_VULCAN_HEBE_GRADE_SUMMARY to LOGIN_METHOD_VULCAN_HEBE
        ), listOf(LOGIN_METHOD_VULCAN_HEBE)),
        // homework
        Feature(LOGIN_TYPE_VULCAN, FEATURE_HOMEWORK, listOf(
                ENDPOINT_VULCAN_HEBE_HOMEWORK to LOGIN_METHOD_VULCAN_HEBE
        ), listOf(LOGIN_METHOD_VULCAN_HEBE)),
        // behaviour
        Feature(LOGIN_TYPE_VULCAN, FEATURE_BEHAVIOUR, listOf(
                ENDPOINT_VULCAN_HEBE_NOTICES to LOGIN_METHOD_VULCAN_HEBE
        ), listOf(LOGIN_METHOD_VULCAN_HEBE)),
        // attendance
        Feature(LOGIN_TYPE_VULCAN, FEATURE_ATTENDANCE, listOf(
                ENDPOINT_VULCAN_HEBE_ATTENDANCE to LOGIN_METHOD_VULCAN_HEBE
        ), listOf(LOGIN_METHOD_VULCAN_HEBE)),
        // messages
        Feature(LOGIN_TYPE_VULCAN, FEATURE_MESSAGES_INBOX, listOf(
                ENDPOINT_VULCAN_HEBE_MESSAGES_INBOX to LOGIN_METHOD_VULCAN_HEBE
        ), listOf(LOGIN_METHOD_VULCAN_HEBE)),
        Feature(LOGIN_TYPE_VULCAN, FEATURE_MESSAGES_SENT, listOf(
                ENDPOINT_VULCAN_HEBE_MESSAGES_SENT to LOGIN_METHOD_VULCAN_HEBE
        ), listOf(LOGIN_METHOD_VULCAN_HEBE)),

        // push config
        Feature(LOGIN_TYPE_VULCAN, FEATURE_PUSH_CONFIG, listOf(
                ENDPOINT_VULCAN_HEBE_PUSH_CONFIG to LOGIN_METHOD_VULCAN_HEBE
        ), listOf(LOGIN_METHOD_VULCAN_HEBE)).withShouldSync { data ->
                !data.app.config.sync.tokenVulcanList.contains(data.profileId)
        },

        /**
         * Lucky number - using WEB Main.
         */
        Feature(LOGIN_TYPE_VULCAN, FEATURE_LUCKY_NUMBER, listOf(
                ENDPOINT_VULCAN_WEB_LUCKY_NUMBERS to LOGIN_METHOD_VULCAN_WEB_MAIN
        ), listOf(LOGIN_METHOD_VULCAN_WEB_MAIN))
                .withShouldSync { data -> data.shouldSyncLuckyNumber() }
                .withPriority(2),
        /**
         * Lucky number - using Hebe API
         */
        Feature(LOGIN_TYPE_VULCAN, FEATURE_LUCKY_NUMBER, listOf(
                ENDPOINT_VULCAN_HEBE_LUCKY_NUMBER to LOGIN_METHOD_VULCAN_HEBE
        ), listOf(LOGIN_METHOD_VULCAN_HEBE))
                .withShouldSync { data -> data.shouldSyncLuckyNumber() }
                .withPriority(1),

        Feature(LOGIN_TYPE_VULCAN, FEATURE_ALWAYS_NEEDED, listOf(
                ENDPOINT_VULCAN_HEBE_MAIN to LOGIN_METHOD_VULCAN_HEBE,
                ENDPOINT_VULCAN_HEBE_ADDRESSBOOK to LOGIN_METHOD_VULCAN_HEBE
        ), listOf(LOGIN_METHOD_VULCAN_HEBE))
)

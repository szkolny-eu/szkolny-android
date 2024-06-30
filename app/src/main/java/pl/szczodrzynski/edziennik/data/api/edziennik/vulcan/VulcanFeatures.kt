/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-6. 
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan

import pl.szczodrzynski.edziennik.data.api.models.Feature
import pl.szczodrzynski.edziennik.data.enums.FeatureType
import pl.szczodrzynski.edziennik.data.enums.LoginMethod
import pl.szczodrzynski.edziennik.data.enums.LoginType

const val ENDPOINT_VULCAN_WEB_LUCKY_NUMBERS       = 2010

const val ENDPOINT_VULCAN_HEBE_MAIN               = 3000
const val ENDPOINT_VULCAN_HEBE_PUSH_CONFIG        = 3005
const val ENDPOINT_VULCAN_HEBE_ADDRESSBOOK        = 3010
const val ENDPOINT_VULCAN_HEBE_ADDRESSBOOK_2      = 3501 // after message boxes (3500)
const val ENDPOINT_VULCAN_HEBE_TIMETABLE          = 3020
const val ENDPOINT_VULCAN_HEBE_EXAMS              = 3030
const val ENDPOINT_VULCAN_HEBE_GRADES             = 3040
const val ENDPOINT_VULCAN_HEBE_GRADE_SUMMARY      = 3050
const val ENDPOINT_VULCAN_HEBE_HOMEWORK           = 3060
const val ENDPOINT_VULCAN_HEBE_NOTICES            = 3070
const val ENDPOINT_VULCAN_HEBE_ATTENDANCE         = 3080
const val ENDPOINT_VULCAN_HEBE_TEACHERS           = 3110
const val ENDPOINT_VULCAN_HEBE_LUCKY_NUMBER       = 3200
const val ENDPOINT_VULCAN_HEBE_MESSAGE_BOXES      = 3500
const val ENDPOINT_VULCAN_HEBE_MESSAGES_INBOX     = 3510
const val ENDPOINT_VULCAN_HEBE_MESSAGES_SENT      = 3520

val VulcanFeatures = listOf(
        // timetable
        Feature(LoginType.VULCAN, FeatureType.TIMETABLE, listOf(
                ENDPOINT_VULCAN_HEBE_TIMETABLE to LoginMethod.VULCAN_HEBE
        )),
        // agenda
        Feature(LoginType.VULCAN, FeatureType.AGENDA, listOf(
                ENDPOINT_VULCAN_HEBE_EXAMS to LoginMethod.VULCAN_HEBE
        )),
        // grades
        Feature(LoginType.VULCAN, FeatureType.GRADES, listOf(
                ENDPOINT_VULCAN_HEBE_GRADES to LoginMethod.VULCAN_HEBE,
                ENDPOINT_VULCAN_HEBE_GRADE_SUMMARY to LoginMethod.VULCAN_HEBE
        )),
        // homework
        Feature(LoginType.VULCAN, FeatureType.HOMEWORK, listOf(
                ENDPOINT_VULCAN_HEBE_HOMEWORK to LoginMethod.VULCAN_HEBE
        )),
        // behaviour
        Feature(LoginType.VULCAN, FeatureType.BEHAVIOUR, listOf(
                ENDPOINT_VULCAN_HEBE_NOTICES to LoginMethod.VULCAN_HEBE
        )),
        // attendance
        Feature(LoginType.VULCAN, FeatureType.ATTENDANCE, listOf(
                ENDPOINT_VULCAN_HEBE_ATTENDANCE to LoginMethod.VULCAN_HEBE
        )),
        // messages
        Feature(LoginType.VULCAN, FeatureType.MESSAGES_INBOX, listOf(
                ENDPOINT_VULCAN_HEBE_MESSAGES_INBOX to LoginMethod.VULCAN_HEBE
        )),
        Feature(LoginType.VULCAN, FeatureType.MESSAGES_SENT, listOf(
                ENDPOINT_VULCAN_HEBE_MESSAGES_SENT to LoginMethod.VULCAN_HEBE
        )),

        // push config
        Feature(LoginType.VULCAN, FeatureType.PUSH_CONFIG, listOf(
                ENDPOINT_VULCAN_HEBE_PUSH_CONFIG to LoginMethod.VULCAN_HEBE
        )).withShouldSync { data ->
                !data.app.config.sync.tokenVulcanList.contains(data.profileId)
        },

        /**
         * Lucky number - using WEB Main.
         */
        Feature(LoginType.VULCAN, FeatureType.LUCKY_NUMBER, listOf(
                ENDPOINT_VULCAN_WEB_LUCKY_NUMBERS to LoginMethod.VULCAN_WEB_MAIN
        ))
                .withShouldSync { data -> data.shouldSyncLuckyNumber() }
                .withPriority(2),
        /**
         * Lucky number - using Hebe API
         */
        Feature(LoginType.VULCAN, FeatureType.LUCKY_NUMBER, listOf(
                ENDPOINT_VULCAN_HEBE_LUCKY_NUMBER to LoginMethod.VULCAN_HEBE
        ))
                .withShouldSync { data -> data.shouldSyncLuckyNumber() }
                .withPriority(1),

        Feature(LoginType.VULCAN, FeatureType.ALWAYS_NEEDED, listOf(
                ENDPOINT_VULCAN_HEBE_MAIN to LoginMethod.VULCAN_HEBE,
                ENDPOINT_VULCAN_HEBE_ADDRESSBOOK to LoginMethod.VULCAN_HEBE,
                ENDPOINT_VULCAN_HEBE_TEACHERS to LoginMethod.VULCAN_HEBE,
                ENDPOINT_VULCAN_HEBE_MESSAGE_BOXES to LoginMethod.VULCAN_HEBE,
                ENDPOINT_VULCAN_HEBE_ADDRESSBOOK_2 to LoginMethod.VULCAN_HEBE,
        ))
)

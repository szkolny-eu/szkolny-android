/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik

import pl.szczodrzynski.edziennik.data.api.models.Feature
import pl.szczodrzynski.edziennik.data.enums.FeatureType
import pl.szczodrzynski.edziennik.data.enums.LoginMethod
import pl.szczodrzynski.edziennik.data.enums.LoginType

const val ENDPOINT_MOBIDZIENNIK_API_MAIN                = 1000
const val ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_INBOX      = 2011
const val ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_SENT       = 2012
const val ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_ALL        = 2019
const val ENDPOINT_MOBIDZIENNIK_WEB_CALENDAR            = 2020
const val ENDPOINT_MOBIDZIENNIK_WEB_GRADES              = 2030
const val ENDPOINT_MOBIDZIENNIK_WEB_NOTICES             = 2040
const val ENDPOINT_MOBIDZIENNIK_WEB_ATTENDANCE          = 2050
const val ENDPOINT_MOBIDZIENNIK_WEB_MANUALS             = 2100
const val ENDPOINT_MOBIDZIENNIK_WEB_ACCOUNT_EMAIL       = 2200
const val ENDPOINT_MOBIDZIENNIK_WEB_HOMEWORK            = 2300 // not used as an endpoint
const val ENDPOINT_MOBIDZIENNIK_WEB_TIMETABLE           = 2400
const val ENDPOINT_MOBIDZIENNIK_API2_MAIN               = 3000

val MobidziennikFeatures = listOf(
        // always synced
        Feature(LoginType.MOBIDZIENNIK, FeatureType.ALWAYS_NEEDED, listOf(
                ENDPOINT_MOBIDZIENNIK_API_MAIN to LoginMethod.MOBIDZIENNIK_WEB,
                ENDPOINT_MOBIDZIENNIK_WEB_ACCOUNT_EMAIL to LoginMethod.MOBIDZIENNIK_WEB
        )), // TODO divide features into separate view IDs (all with API_MAIN)

        // push config
        Feature(LoginType.MOBIDZIENNIK, FeatureType.PUSH_CONFIG, listOf(
                ENDPOINT_MOBIDZIENNIK_API2_MAIN to LoginMethod.MOBIDZIENNIK_API2
        )).withShouldSync { data ->
                !data.app.config.sync.tokenMobidziennikList.contains(data.profileId)
        },





        /**
         * Timetable - web scraping - does nothing if the API_MAIN timetable is enough.
         */
        Feature(LoginType.MOBIDZIENNIK, FeatureType.TIMETABLE, listOf(
                ENDPOINT_MOBIDZIENNIK_WEB_TIMETABLE to LoginMethod.MOBIDZIENNIK_WEB
        )),
        /**
         * Agenda - "API" + web scraping.
         */
        Feature(LoginType.MOBIDZIENNIK, FeatureType.AGENDA, listOf(
                ENDPOINT_MOBIDZIENNIK_API_MAIN to LoginMethod.MOBIDZIENNIK_WEB,
                ENDPOINT_MOBIDZIENNIK_WEB_CALENDAR to LoginMethod.MOBIDZIENNIK_WEB
        )),
        /**
         * Grades - "API" + web scraping.
         */
        Feature(LoginType.MOBIDZIENNIK, FeatureType.GRADES, listOf(
                ENDPOINT_MOBIDZIENNIK_API_MAIN to LoginMethod.MOBIDZIENNIK_WEB,
                ENDPOINT_MOBIDZIENNIK_WEB_GRADES to LoginMethod.MOBIDZIENNIK_WEB
        )),
        /**
         * Behaviour - "API" + web scraping.
         */
        Feature(LoginType.MOBIDZIENNIK, FeatureType.BEHAVIOUR, listOf(
                ENDPOINT_MOBIDZIENNIK_API_MAIN to LoginMethod.MOBIDZIENNIK_WEB,
                ENDPOINT_MOBIDZIENNIK_WEB_NOTICES to LoginMethod.MOBIDZIENNIK_WEB
        )),
        /**
         * Attendance - only web scraping.
         */
        Feature(LoginType.MOBIDZIENNIK, FeatureType.ATTENDANCE, listOf(
                ENDPOINT_MOBIDZIENNIK_WEB_ATTENDANCE to LoginMethod.MOBIDZIENNIK_WEB
        )),





        /**
         * Messages inbox - using web scraper.
         */
        Feature(LoginType.MOBIDZIENNIK, FeatureType.MESSAGES_INBOX, listOf(
                ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_INBOX to LoginMethod.MOBIDZIENNIK_WEB,
                ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_ALL to LoginMethod.MOBIDZIENNIK_WEB
        )),
        /**
         * Messages sent - using web scraper.
         */
        Feature(LoginType.MOBIDZIENNIK, FeatureType.MESSAGES_SENT, listOf(
                ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_SENT to LoginMethod.MOBIDZIENNIK_WEB,
                ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_ALL to LoginMethod.MOBIDZIENNIK_WEB
        ))

        // lucky number possibilities
        // all endpoints that may supply the lucky number
        /*Feature(LoginType.MOBIDZIENNIK, FeatureType.LUCKY_NUMBER, listOf(
                ENDPOINT_MOBIDZIENNIK_WEB_MANUALS to LoginMethod.MOBIDZIENNIK_WEB
        )).apply { priority = 10 },

        Feature(LoginType.MOBIDZIENNIK, FeatureType.LUCKY_NUMBER, listOf(
                ENDPOINT_MOBIDZIENNIK_WEB_CALENDAR to LoginMethod.MOBIDZIENNIK_WEB
        )).apply { priority = 3 },

        Feature(LoginType.MOBIDZIENNIK, FeatureType.LUCKY_NUMBER, listOf(
                ENDPOINT_MOBIDZIENNIK_WEB_GRADES to LoginMethod.MOBIDZIENNIK_WEB
        )).apply { priority = 2 },

        Feature(LoginType.MOBIDZIENNIK, FeatureType.LUCKY_NUMBER, listOf(
                ENDPOINT_MOBIDZIENNIK_WEB_NOTICES to LoginMethod.MOBIDZIENNIK_WEB
        )).apply { priority = 1 },

        Feature(LoginType.MOBIDZIENNIK, FeatureType.LUCKY_NUMBER, listOf(
                ENDPOINT_MOBIDZIENNIK_WEB_ATTENDANCE to LoginMethod.MOBIDZIENNIK_WEB
        )).apply { priority = 4 }*/

)

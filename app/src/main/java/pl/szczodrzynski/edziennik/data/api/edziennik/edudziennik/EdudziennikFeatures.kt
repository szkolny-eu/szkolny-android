/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-23
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik

import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.models.Feature

const val ENDPOINT_EDUDZIENNIK_WEB_START                = 1000
const val ENDPOINT_EDUDZIENNIK_WEB_TIMETABLE            = 1001
const val ENDPOINT_EDUDZIENNIK_WEB_LUCKY_NUMBER         = 1010

val EdudziennikFeatures = listOf(
        Feature(LOGIN_TYPE_EDUDZIENNIK, FEATURE_STUDENT_INFO, listOf(
                ENDPOINT_EDUDZIENNIK_WEB_START to LOGIN_METHOD_EDUDZIENNIK_WEB
        ), listOf(LOGIN_METHOD_EDUDZIENNIK_WEB)),

        Feature(LOGIN_TYPE_EDUDZIENNIK, FEATURE_TIMETABLE, listOf(
                ENDPOINT_EDUDZIENNIK_WEB_TIMETABLE to LOGIN_METHOD_EDUDZIENNIK_WEB
        ), listOf(LOGIN_METHOD_EDUDZIENNIK_WEB)),

        Feature(LOGIN_TYPE_EDUDZIENNIK, FEATURE_GRADES, listOf(
                ENDPOINT_EDUDZIENNIK_WEB_START to LOGIN_METHOD_EDUDZIENNIK_WEB
        ), listOf(LOGIN_METHOD_EDUDZIENNIK_WEB)),

        Feature(LOGIN_TYPE_EDUDZIENNIK, FEATURE_LUCKY_NUMBER, listOf(
                ENDPOINT_EDUDZIENNIK_WEB_LUCKY_NUMBER to LOGIN_METHOD_EDUDZIENNIK_WEB
        ), listOf(LOGIN_METHOD_EDUDZIENNIK_WEB))
)

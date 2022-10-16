/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-11.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.usos

import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.models.Feature

const val ENDPOINT_USOS_API_USER        = 7000
const val ENDPOINT_USOS_API_TERMS       = 7010
const val ENDPOINT_USOS_API_COURSES     = 7020
const val ENDPOINT_USOS_API_TIMETABLE   = 7030

val UsosFeatures = listOf(
    /*
    * Student information
    */
    Feature(LOGIN_TYPE_USOS, FEATURE_STUDENT_INFO, listOf(
        ENDPOINT_USOS_API_USER to LOGIN_METHOD_USOS_API,
    ), listOf(LOGIN_METHOD_USOS_API)),

    /*
    * Terms & courses
    */
    Feature(LOGIN_TYPE_USOS, FEATURE_SCHOOL_INFO, listOf(
        ENDPOINT_USOS_API_TERMS to LOGIN_METHOD_USOS_API,
    ), listOf(LOGIN_METHOD_USOS_API)),
    Feature(LOGIN_TYPE_USOS, FEATURE_TEAM_INFO, listOf(
        ENDPOINT_USOS_API_COURSES to LOGIN_METHOD_USOS_API,
    ), listOf(LOGIN_METHOD_USOS_API)),

    /*
     * Timetable
     */
    Feature(LOGIN_TYPE_USOS, FEATURE_TIMETABLE, listOf(
        ENDPOINT_USOS_API_TIMETABLE to LOGIN_METHOD_USOS_API,
    ), listOf(LOGIN_METHOD_USOS_API)),
)

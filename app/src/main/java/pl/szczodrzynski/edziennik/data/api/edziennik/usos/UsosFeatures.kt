/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-11.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.usos

import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.FEATURE_ALWAYS_NEEDED
import pl.szczodrzynski.edziennik.data.api.FEATURE_STUDENT_INFO
import pl.szczodrzynski.edziennik.data.api.FEATURE_TEAM_INFO
import pl.szczodrzynski.edziennik.data.api.models.Feature

const val ENDPOINT_USOS_API_USER = 7000
const val ENDPOINT_USOS_API_TERMS = 7010
const val ENDPOINT_USOS_API_COURSES = 7020

val UsosFeatures = listOf(
    Feature(LOGIN_TYPE_USOS, FEATURE_STUDENT_INFO, listOf(
        ENDPOINT_USOS_API_USER to LOGIN_METHOD_USOS_API,
    ), listOf(LOGIN_METHOD_USOS_API)),

    Feature(LOGIN_TYPE_USOS, FEATURE_SCHOOL_INFO, listOf(
        ENDPOINT_USOS_API_TERMS to LOGIN_METHOD_USOS_API,
    ), listOf(LOGIN_METHOD_USOS_API)),

    Feature(LOGIN_TYPE_USOS, FEATURE_TEAM_INFO, listOf(
        ENDPOINT_USOS_API_COURSES to LOGIN_METHOD_USOS_API,
    ), listOf(LOGIN_METHOD_USOS_API)),
)

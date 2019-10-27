/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-25. 
 */

package pl.szczodrzynski.edziennik.api.v2.idziennik

import pl.szczodrzynski.edziennik.api.v2.*
import pl.szczodrzynski.edziennik.api.v2.models.Feature

const val ENDPOINT_IDZIENNIK_WEB_SAMPLE                  = 9991
const val ENDPOINT_IDZIENNIK_WEB_SAMPLE_2                = 9992
const val ENDPOINT_IDZIENNIK_API_SAMPLE                  = 9993

val IdziennikFeatures = listOf(
        Feature(LOGIN_TYPE_IDZIENNIK, FEATURE_STUDENT_INFO, listOf(
                ENDPOINT_IDZIENNIK_WEB_SAMPLE to LOGIN_METHOD_IDZIENNIK_WEB
        ), listOf(LOGIN_METHOD_IDZIENNIK_WEB)),
        Feature(LOGIN_TYPE_IDZIENNIK, FEATURE_SCHOOL_INFO, listOf(
                ENDPOINT_IDZIENNIK_WEB_SAMPLE_2 to LOGIN_METHOD_IDZIENNIK_WEB
        ), listOf(LOGIN_METHOD_IDZIENNIK_WEB)),
        Feature(LOGIN_TYPE_IDZIENNIK, FEATURE_GRADES, listOf(
                ENDPOINT_IDZIENNIK_API_SAMPLE to LOGIN_METHOD_IDZIENNIK_API
        ), listOf(LOGIN_METHOD_IDZIENNIK_API))
)

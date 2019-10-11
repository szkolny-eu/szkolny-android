/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-11.
 */

package pl.szczodrzynski.edziennik.api.v2.template

import pl.szczodrzynski.edziennik.api.v2.*
import pl.szczodrzynski.edziennik.api.v2.models.Feature

const val ENDPOINT_TEMPLATE_WEB_SAMPLE                  = 9991
const val ENDPOINT_TEMPLATE_WEB_SAMPLE_2                = 9992
const val ENDPOINT_TEMPLATE_API_SAMPLE                  = 9993

val TemplateFeatures = listOf(
        Feature(LOGIN_TYPE_TEMPLATE, FEATURE_STUDENT_INFO, listOf(
                ENDPOINT_TEMPLATE_WEB_SAMPLE to LOGIN_METHOD_TEMPLATE_WEB
        ), listOf(LOGIN_METHOD_TEMPLATE_WEB)),
        Feature(LOGIN_TYPE_TEMPLATE, FEATURE_SCHOOL_INFO, listOf(
                ENDPOINT_TEMPLATE_WEB_SAMPLE_2 to LOGIN_METHOD_TEMPLATE_WEB
        ), listOf(LOGIN_METHOD_TEMPLATE_WEB)),
        Feature(LOGIN_TYPE_TEMPLATE, FEATURE_GRADES, listOf(
                ENDPOINT_TEMPLATE_API_SAMPLE to LOGIN_METHOD_TEMPLATE_API
        ), listOf(LOGIN_METHOD_TEMPLATE_API))
)
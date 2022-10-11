/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-11.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.usos

import pl.szczodrzynski.edziennik.data.api.FEATURE_STUDENT_INFO
import pl.szczodrzynski.edziennik.data.api.LOGIN_METHOD_USOS_API
import pl.szczodrzynski.edziennik.data.api.LOGIN_TYPE_USOS
import pl.szczodrzynski.edziennik.data.api.models.Feature

const val ENDPOINT_USOS_API_USER = 7000

val UsosFeatures = listOf(
    Feature(LOGIN_TYPE_USOS, FEATURE_STUDENT_INFO, listOf(
        ENDPOINT_USOS_API_USER to LOGIN_METHOD_USOS_API,
    ), listOf(LOGIN_METHOD_USOS_API)),
)

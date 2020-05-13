/*
 * Copyright (c) Kacper Ziubryniewicz 2020-5-12
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.podlasie

import pl.szczodrzynski.edziennik.data.api.FEATURE_ALWAYS_NEEDED
import pl.szczodrzynski.edziennik.data.api.LOGIN_METHOD_PODLASIE_API
import pl.szczodrzynski.edziennik.data.api.LOGIN_TYPE_PODLASIE
import pl.szczodrzynski.edziennik.data.api.models.Feature

const val ENDPOINT_PODLASIE_API_MAIN = 1001

val PodlasieFeatures = listOf(
        Feature(LOGIN_TYPE_PODLASIE, FEATURE_ALWAYS_NEEDED, listOf(
                ENDPOINT_PODLASIE_API_MAIN to LOGIN_METHOD_PODLASIE_API
        ), listOf(LOGIN_METHOD_PODLASIE_API))
)

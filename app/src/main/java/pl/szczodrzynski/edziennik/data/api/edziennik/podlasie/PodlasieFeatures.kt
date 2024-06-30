/*
 * Copyright (c) Kacper Ziubryniewicz 2020-5-12
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.podlasie

import pl.szczodrzynski.edziennik.data.api.models.Feature
import pl.szczodrzynski.edziennik.data.enums.FeatureType
import pl.szczodrzynski.edziennik.data.enums.LoginMethod
import pl.szczodrzynski.edziennik.data.enums.LoginType

const val ENDPOINT_PODLASIE_API_MAIN = 1001

val PodlasieFeatures = listOf(
        Feature(LoginType.PODLASIE, FeatureType.ALWAYS_NEEDED, listOf(
                ENDPOINT_PODLASIE_API_MAIN to LoginMethod.PODLASIE_API
        ))
)

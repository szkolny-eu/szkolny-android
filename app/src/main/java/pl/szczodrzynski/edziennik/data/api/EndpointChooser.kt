package pl.szczodrzynski.edziennik.data.api

import pl.szczodrzynski.edziennik.data.api.models.Data
import pl.szczodrzynski.edziennik.data.api.models.Feature
import pl.szczodrzynski.edziennik.data.db.entity.EndpointTimer
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_NEVER
import pl.szczodrzynski.edziennik.data.db.enums.FeatureType
import pl.szczodrzynski.edziennik.data.db.enums.LoginMethod
import pl.szczodrzynski.edziennik.data.db.enums.LoginType
import pl.szczodrzynski.edziennik.ext.getFeatureTypesNecessary
import pl.szczodrzynski.edziennik.ext.getFeatureTypesUnnecessary
import pl.szczodrzynski.edziennik.ext.isNotNullNorEmpty

fun Data.prepare(
    features: List<Feature>,
    featureTypes: Set<FeatureType>?,
    viewId: Int?,
    onlyEndpoints: List<Int>?,
) {
    val loginType = this.loginStore.type
    val possibleLoginMethods = this.loginMethods.toMutableList()
    possibleLoginMethods += LoginMethod.values().filter {
        it.loginType == loginType && it.isPossible?.invoke(profile, loginStore) != false
    }

    //var highestLoginMethod = 0
    var possibleFeatures = mutableListOf<Feature>()
    val requiredLoginMethods = mutableListOf<LoginMethod>()

    val syncFeatureTypes = when {
        featureTypes.isNotNullNorEmpty() -> featureTypes!!
        else -> getFeatureTypesUnnecessary()
    } + getFeatureTypesNecessary()

    this.targetEndpoints.clear()
    this.targetLoginMethods.clear()

    // get all endpoints for every feature, only if possible to login and possible/necessary to sync
    for (featureId in syncFeatureTypes) {
        possibleFeatures += features.filter {
            it.featureType == featureId // feature ID matches
                    && possibleLoginMethods.containsAll(it.requiredLoginMethods) // is possible to login
                    && it.shouldSync?.invoke(this) ?: true // is necessary/possible to sync
        }
    }

    val timestamp = System.currentTimeMillis()

    possibleFeatures = possibleFeatures
        // sort the endpoint list by feature ID and priority
        .sortedWith(compareBy(Feature::featureType, Feature::priority))
        // select only the most important endpoint for each feature
        .distinctBy { it.featureType }
        .toMutableList()

    for (feature in possibleFeatures) {
        // add all endpoint IDs and required login methods, filtering using timers
        feature.endpoints.forEach { endpoint ->
            if (onlyEndpoints?.contains(endpoint.first) == false)
                return@forEach
            val timer = this.endpointTimers
                .singleOrNull { it.endpointId == endpoint.first }
                ?: EndpointTimer(this.profileId, endpoint.first)
            if (
                onlyEndpoints?.contains(endpoint.first) == true ||
                timer.nextSync == SYNC_ALWAYS ||
                viewId != null && timer.viewId == viewId ||
                timer.nextSync != SYNC_NEVER && timer.nextSync < timestamp
            ) {
                this.targetEndpoints[endpoint.first] = timer.lastSync
                requiredLoginMethods += endpoint.second
            }
        }
    }

    // check every login method for any dependencies
    for (loginMethod in requiredLoginMethods) {
        var requiredLoginMethod: LoginMethod? = loginMethod
        while (requiredLoginMethod != null) {
            this.targetLoginMethods += requiredLoginMethod
            requiredLoginMethod = requiredLoginMethod.requiredLoginMethod?.invoke(this.profile, this.loginStore)
        }
    }

    // sort and distinct every login method and endpoint
    this.targetLoginMethods = this.targetLoginMethods.toHashSet().toMutableList()
    this.targetLoginMethods.sort()

    //data.targetEndpointIds = data.targetEndpointIds.toHashSet().toMutableList()
    //data.targetEndpointIds.sort()

    progressCount = targetLoginMethods.size + targetEndpoints.size
    progressStep = if (progressCount <= 0) 0f else 100f / progressCount.toFloat()
}

fun Data.prepareFor(loginMethod: LoginMethod) {
    val loginType = loginStore.type
    val possibleLoginMethods = this.loginMethods.toMutableList()
    possibleLoginMethods += LoginMethod.values().filter {
        it.loginType == loginType && it.isPossible?.invoke(profile, loginStore) != false
    }

    this.targetLoginMethods.clear()

    // check the login method for any dependencies
    var requiredLoginMethod: LoginMethod? = loginMethod
    while (requiredLoginMethod != null) {
        this.targetLoginMethods += requiredLoginMethod
        requiredLoginMethod = requiredLoginMethod.requiredLoginMethod?.invoke(this.profile, this.loginStore)
    }

    // sort and distinct every login method
    this.targetLoginMethods = this.targetLoginMethods.toHashSet().toMutableList()
    this.targetLoginMethods.sort()

    progressCount = 0
    progressStep = 0f
}

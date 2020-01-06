package pl.szczodrzynski.edziennik.data.api

import pl.szczodrzynski.edziennik.data.api.models.Data
import pl.szczodrzynski.edziennik.data.api.models.Feature
import pl.szczodrzynski.edziennik.data.api.models.LoginMethod
import pl.szczodrzynski.edziennik.data.db.entity.EndpointTimer
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_NEVER

fun Data.prepare(loginMethods: List<LoginMethod>, features: List<Feature>, featureIds: List<Int>, viewId: Int?) {
    val data = this

    val possibleLoginMethods = data.loginMethods.toMutableList()

    for (loginMethod in loginMethods) {
        if (loginMethod.isPossible(profile, loginStore))
            possibleLoginMethods += loginMethod.loginMethodId
    }

    //var highestLoginMethod = 0
    var endpointList = mutableListOf<Feature>()
    val requiredLoginMethods = mutableListOf<Int>()

    data.targetEndpointIds.clear()
    data.targetLoginMethodIds.clear()

    // get all endpoints for every feature, only if possible to login and possible/necessary to sync
    for (featureId in featureIds) {
        features.filter {
            it.featureId == featureId // feature ID matches
                    && possibleLoginMethods.containsAll(it.requiredLoginMethods) // is possible to login
                    && it.shouldSync?.invoke(data) ?: true // is necessary/possible to sync
        }.let {
            endpointList.addAll(it)
        }
    }

    val timestamp = System.currentTimeMillis()

    endpointList = endpointList
            // sort the endpoint list by feature ID and priority
            .sortedWith(compareBy(Feature::featureId, Feature::priority))
            // select only the most important endpoint for each feature
            .distinctBy { it.featureId }
            .toMutableList()
            // add all endpoint IDs and required login methods, filtering using timers
            .onEach { feature ->
                feature.endpointIds.forEach { endpoint ->
                    (data.endpointTimers
                            .singleOrNull { it.endpointId == endpoint.first } ?: EndpointTimer(data.profile?.id
                            ?: -1, endpoint.first))
                            .let { timer ->
                                if (timer.nextSync == SYNC_ALWAYS ||
                                        (viewId != null && timer.viewId == viewId) ||
                                        (timer.nextSync != SYNC_NEVER && timer.nextSync < timestamp)) {
                                    data.targetEndpointIds.add(endpoint.first)
                                    requiredLoginMethods.add(endpoint.second)
                                }
                            }
                }
            }

    // check every login method for any dependencies
    for (loginMethodId in requiredLoginMethods) {
        var requiredLoginMethod: Int? = loginMethodId
        while (requiredLoginMethod != LOGIN_METHOD_NOT_NEEDED) {
            loginMethods.singleOrNull { it.loginMethodId == requiredLoginMethod }?.let { loginMethod ->
                if (requiredLoginMethod != null)
                    data.targetLoginMethodIds.add(requiredLoginMethod!!)
                requiredLoginMethod = loginMethod.requiredLoginMethod(data.profile, data.loginStore)
            }
        }
    }

    // sort and distinct every login method and endpoint
    data.targetLoginMethodIds = data.targetLoginMethodIds.toHashSet().toMutableList()
    data.targetLoginMethodIds.sort()

    data.targetEndpointIds = data.targetEndpointIds.toHashSet().toMutableList()
    data.targetEndpointIds.sort()

    progressCount = targetLoginMethodIds.size + targetEndpointIds.size
    progressStep = if (progressCount <= 0) 0f else 100f / progressCount.toFloat()
}

fun Data.prepareFor(loginMethods: List<LoginMethod>, loginMethodId: Int) {
    val possibleLoginMethods = this.loginMethods.toMutableList()

    loginMethods.forEach {
        if (it.isPossible(profile, loginStore))
            possibleLoginMethods += it.loginMethodId
    }

    targetEndpointIds.clear()
    targetLoginMethodIds.clear()

    // check the login method for any dependencies
    var requiredLoginMethod: Int? = loginMethodId
    while (requiredLoginMethod != LOGIN_METHOD_NOT_NEEDED) {
        loginMethods.singleOrNull { it.loginMethodId == requiredLoginMethod }?.let {
            if (requiredLoginMethod != null)
                targetLoginMethodIds.add(requiredLoginMethod!!)
            requiredLoginMethod = it.requiredLoginMethod(profile, loginStore)
        }
    }

    // sort and distinct every login method
    targetLoginMethodIds = targetLoginMethodIds.toHashSet().toMutableList()
    targetLoginMethodIds.sort()

    progressCount = 0
    progressStep = 0f
}

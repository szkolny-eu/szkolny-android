package pl.szczodrzynski.edziennik.data.api.models

import pl.szczodrzynski.edziennik.data.enums.FeatureType
import pl.szczodrzynski.edziennik.data.enums.LoginMethod
import pl.szczodrzynski.edziennik.data.enums.LoginType

/**
 * A Endpoint descriptor class.
 *
 * The API runs appropriate endpoints in order to fulfill its
 * feature list.
 * An endpoint may have its [LoginMethod] dependencies which will be
 * satisfied by the API before the endpoint class is invoked.
 *
 * @param loginType type of the e-register this endpoint handles
 * @param featureType type of the feature
 * @param endpoints a [List] of endpoints and their required login methods that satisfy this feature type
 */
data class Feature(
    val loginType: LoginType,
    val featureType: FeatureType,
    val endpoints: List<Pair<Int, LoginMethod>>,
) {
    var priority = endpoints.size
    fun withPriority(priority: Int): Feature {
        this.priority = priority
        return this
    }

    var shouldSync: ((Data) -> Boolean)? = null
    fun withShouldSync(shouldSync: ((Data) -> Boolean)?): Feature {
        this.shouldSync = shouldSync
        return this
    }

    val requiredLoginMethods by lazy { endpoints.map { it.second } }
}

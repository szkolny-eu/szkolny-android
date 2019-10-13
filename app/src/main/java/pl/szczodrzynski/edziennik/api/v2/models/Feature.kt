package pl.szczodrzynski.edziennik.api.v2.models

/**
 * A Endpoint descriptor class.
 *
 * The API runs appropriate endpoints in order to fulfill its
 * feature list.
 * An endpoint may have its [LoginMethod] dependencies which will be
 * satisfied by the API before the [endpointClass]'s constructor is invoked.
 *
 * @param loginType type of the e-register this endpoint handles
 * @param featureId a feature ID
 * @param endpointIds a [List] of [Feature]s that satisfy this feature ID
 * @param requiredLoginMethod a required login method, which will have to be executed before this endpoint.
 */
data class Feature(
        val loginType: Int,
        val featureId: Int,
        val endpointIds: List<Pair<Int, Int>>,
        val requiredLoginMethods: List<Int>
) {
    var priority = endpointIds.size
    fun withPriority(priority: Int): Feature {
        this.priority = priority
        return this
    }

    var shouldSync: ((Data) -> Boolean)? = null
    fun withShouldSync(shouldSync: ((Data) -> Boolean)?): Feature {
        this.shouldSync = shouldSync
        return this
    }
}
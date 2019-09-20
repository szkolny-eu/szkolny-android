package pl.szczodrzynski.edziennik.api.v2.models

import pl.szczodrzynski.edziennik.datamodels.LoginStore
import pl.szczodrzynski.edziennik.datamodels.Profile

/**
 * A Endpoint descriptor class.
 *
 * The API runs appropriate endpoints in order to fulfill its
 * [Feature] list.
 * An endpoint may have its [LoginMethod] dependencies which will be
 * satisfied by the API before the [endpointClass]'s constructor is invoked.
 *
 * @param loginType type of the e-register this endpoint handles
 * @param endpointId a unique ID of this endpoint
 * @param featureIds a [List] of [Feature]s (their IDs) this endpoint can download
 * @param endpointClass a [Class] which constructor will be invoked when a data download is needed
 * @param requiredLoginMethod a lambda returning a required login method (which will be called before this). May differ depending on the [Profile] and/or [LoginStore].
 */
class Endpoint(
        val loginType: Int,
        val endpointId: Int,
        val featureIds: List<Int>,
        val endpointClass: Class<*>,
        val requiredLoginMethod: (profile: Profile?, loginStore: LoginStore) -> Int
)
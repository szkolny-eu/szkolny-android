/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-20.
 */

package pl.szczodrzynski.edziennik.api.v2.models

import pl.szczodrzynski.edziennik.datamodels.LoginStore
import pl.szczodrzynski.edziennik.datamodels.Profile

/**
 * A Login Method descriptor class.
 *
 * This is used by the API to satisfy all [Endpoint]s' dependencies.
 * A login method may have its own dependencies which need to be
 * satisfied before the [loginMethodClass]'s constructor is invoked.
 *
 * @param loginType type of the e-register this login method handles
 * @param loginMethodId a unique ID of this login method
 * @param featureIds a [List] of [Feature]s (their IDs) this login method can provide access to
 * @param loginMethodClass a [Class] which constructor will be invoked when a log in is needed
 * @param requiredLoginMethod a lambda returning a required login method (which will be called before this). May differ depending on the [Profile] and/or [LoginStore].
 */
class LoginMethod(
        val loginType: Int,
        val loginMethodId: Int,
        val featureIds: List<Int>,
        val loginMethodClass: Class<*>,
        val requiredLoginMethod: (profile: Profile?, loginStore: LoginStore) -> Int
)
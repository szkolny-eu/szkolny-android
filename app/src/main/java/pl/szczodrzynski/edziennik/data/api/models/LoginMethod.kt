/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-20.
 */

package pl.szczodrzynski.edziennik.data.api.models

import pl.szczodrzynski.edziennik.data.api.LOGIN_METHOD_NOT_NEEDED
import pl.szczodrzynski.edziennik.data.db.modules.login.LoginStore
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile

/**
 * A Login Method descriptor class.
 *
 * This is used by the API to satisfy all [Feature]s' dependencies.
 * A login method may have its own dependencies which need to be
 * satisfied before the [loginMethodClass]'s constructor is invoked.
 *
 * @param loginType type of the e-register this login method handles
 * @param loginMethodId a unique ID of this login method
 * @param loginMethodClass a [Class] which constructor will be invoked when a log in is needed
 * @param requiredLoginMethod a required login method (which will be called before this). May differ depending on the [Profile] and/or [LoginStore].
 */
class LoginMethod(
        val loginType: Int,
        val loginMethodId: Int,
        val loginMethodClass: Class<*>,
        private var mIsPossible: ((profile: Profile?, loginStore: LoginStore) -> Boolean)? = null,
        private var mRequiredLoginMethod: ((profile: Profile?, loginStore: LoginStore) -> Int)? = null
) {

    fun withIsPossible(isPossible: (profile: Profile?, loginStore: LoginStore) -> Boolean): LoginMethod {
        this.mIsPossible = isPossible
        return this
    }
    fun withRequiredLoginMethod(requiredLoginMethod: (profile: Profile?, loginStore: LoginStore) -> Int): LoginMethod {
        this.mRequiredLoginMethod = requiredLoginMethod
        return this
    }

    fun isPossible(profile: Profile?, loginStore: LoginStore): Boolean {
        return mIsPossible?.invoke(profile, loginStore) ?: false
    }
    fun requiredLoginMethod(profile: Profile?, loginStore: LoginStore): Int {
        return mRequiredLoginMethod?.invoke(profile, loginStore) ?: LOGIN_METHOD_NOT_NEEDED
    }
}

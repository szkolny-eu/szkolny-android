/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-21.
 */

package pl.szczodrzynski.edziennik.api.v2.librus.login

import pl.szczodrzynski.edziennik.api.v2.*
import pl.szczodrzynski.edziennik.api.v2.librus.data.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.models.LoginMethod
import pl.szczodrzynski.edziennik.utils.Utils.d
import kotlin.math.log

class LoginLibrus(val data: DataLibrus, vararg loginMethodIds: Int, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "LoginLibrus"
    }

    private var loginMethodList = mutableListOf<Int>()

    init {
        for (loginMethodId in loginMethodIds) {
            var requiredLoginMethod: Int? = loginMethodId
            while (requiredLoginMethod != LOGIN_METHOD_NOT_NEEDED) {
                librusLoginMethods.singleOrNull { it.loginMethodId == requiredLoginMethod }?.let { loginMethod ->
                    if (requiredLoginMethod != null)
                        loginMethodList.add(requiredLoginMethod!!)
                    requiredLoginMethod = loginMethod.requiredLoginMethod(data.profile, data.loginStore)
                }
            }
        }
        loginMethodList = loginMethodList.toHashSet().toMutableList()
        loginMethodList.sort()

        data.satisfyLoginMethods()
        nextLoginMethod()
    }

    private fun nextLoginMethod() {
        if (loginMethodList.isEmpty()) {
            onSuccess()
            return
        }
        useLoginMethod(loginMethodList.removeAt(0)) {
            nextLoginMethod()
        }
    }

    private fun useLoginMethod(loginMethodId: Int, onSuccess: () -> Unit) {
        if (data.loginMethods.contains(loginMethodId)) {
            onSuccess()
            return
        }
        d(TAG, "Using login method $loginMethodId")
        when (loginMethodId) {
            LOGIN_METHOD_LIBRUS_PORTAL -> {
                LoginLibrusPortal(data) {
                    data.loginMethods.add(loginMethodId)
                    onSuccess()
                }
            }
            LOGIN_METHOD_LIBRUS_API -> {
                LoginLibrusApi(data) {
                    data.loginMethods.add(loginMethodId)
                    onSuccess()
                }
            }
            LOGIN_METHOD_LIBRUS_SYNERGIA -> {
                LoginLibrusSynergia(data) {
                    data.loginMethods.add(loginMethodId)
                    onSuccess()
                }
            }
            LOGIN_METHOD_LIBRUS_MESSAGES -> {
                LoginLibrusMessages(data) {
                    data.loginMethods.add(loginMethodId)
                    onSuccess()
                }
            }
        }
    }
}
/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-20.
 */

package pl.szczodrzynski.edziennik.api.v2.librus.login

import pl.szczodrzynski.edziennik.api.v2.*
import pl.szczodrzynski.edziennik.api.v2.librus.data.DataLibrus
import pl.szczodrzynski.edziennik.currentTimeUnix
import pl.szczodrzynski.edziennik.isNotNullNorEmpty

class LoginLibrusMessages(val data: DataLibrus, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "LoginLibrusMessages"
    }

    init { run {
        if (data.profile == null) {
            data.error(TAG, ERROR_PROFILE_MISSING)
            return@run
        }

        if (data.messagesSessionIdExpiryTime-30 > currentTimeUnix() && data.messagesSessionId.isNotNullNorEmpty()) {
            onSuccess()
        }
        else {
            when (data.loginStore.mode) {
                LOGIN_MODE_LIBRUS_SYNERGIA -> loginWithCredentials()
                else -> {
                    loginWithSynergia()
                }
            }
        }
    }}

    private fun loginWithCredentials() {
        if (data.apiLogin == null || data.apiPassword == null) {
            if (!data.loginMethods.contains(LOGIN_METHOD_LIBRUS_SYNERGIA)) {
                data.error(TAG, ERROR_LOGIN_METHOD_NOT_SATISFIED)
                return
            }
            data.error(TAG, ERROR_LOGIN_DATA_MISSING)
            return
        }
    }
}
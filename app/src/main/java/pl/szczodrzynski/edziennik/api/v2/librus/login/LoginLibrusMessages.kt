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

        if (data.isMessagesLoginValid()) {
            onSuccess()
        }
        else {
            if (data.loginMethods.contains(LOGIN_METHOD_LIBRUS_SYNERGIA)) {
                loginWithSynergia()
            }
            else if (data.apiLogin != null && data.apiPassword != null) {
                loginWithCredentials()
            }
            else {
                data.error(TAG, ERROR_LOGIN_DATA_MISSING)
            }
        }
    }}

    /**
     * XML (Flash messages website) login method. Uses a Synergia login and password.
     */
    private fun loginWithCredentials() {

    }

    /**
     * A login method using the Synergia website (/wiadomosci2 Auto Login).
     */
    private fun loginWithSynergia() {

    }
}
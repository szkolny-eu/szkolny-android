/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-22
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.login

import okhttp3.Cookie
import pl.szczodrzynski.edziennik.currentTimeUnix
import pl.szczodrzynski.edziennik.data.api.ERROR_LOGIN_DATA_MISSING
import pl.szczodrzynski.edziennik.data.api.ERROR_PROFILE_MISSING
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.models.ApiError

class EdudziennikLoginWeb(val data: DataEdudziennik, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "EdudziennikLoginWeb"
    }

    init { run {
        if (data.profile == null) {
            data.error(ApiError(TAG, ERROR_PROFILE_MISSING))
            return@run
        }

        if (data.isWebLoginValid()) {
            data.app.cookieJar.saveFromResponse(null, listOf(
                    Cookie.Builder()
                            .name("AuthCookie")
                            .value(data.webCookie!!)
                            .domain("eregister.example.com")
                            .secure().httpOnly().build()
            ))
            onSuccess()
        }
        else {
            data.app.cookieJar.clearForDomain("eregister.example.com")
            if (/*data.webLogin != null && data.webPassword != null && */true) {
                loginWithCredentials()
            }
            else {
                data.error(ApiError(TAG, ERROR_LOGIN_DATA_MISSING))
            }
        }
    }}

    fun loginWithCredentials() {
        // succeed immediately

        data.webCookie = "ThisIsACookie"
        data.webExpiryTime = currentTimeUnix() + 45 * 60 /* 45min */
        onSuccess()
    }
}

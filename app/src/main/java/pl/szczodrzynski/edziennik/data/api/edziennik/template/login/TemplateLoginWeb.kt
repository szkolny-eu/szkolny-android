/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.template.login

import okhttp3.Cookie
import pl.szczodrzynski.edziennik.data.api.ERROR_LOGIN_DATA_MISSING
import pl.szczodrzynski.edziennik.data.api.ERROR_PROFILE_MISSING
import pl.szczodrzynski.edziennik.data.api.edziennik.template.DataTemplate
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.currentTimeUnix

class TemplateLoginWeb(val data: DataTemplate, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "TemplateLoginWeb"
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

/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.template.login

import pl.szczodrzynski.edziennik.currentTimeUnix
import pl.szczodrzynski.edziennik.data.api.ERROR_LOGIN_DATA_MISSING
import pl.szczodrzynski.edziennik.data.api.edziennik.template.DataTemplate
import pl.szczodrzynski.edziennik.data.api.models.ApiError

class TemplateLoginWeb(val data: DataTemplate, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "TemplateLoginWeb"
    }

    init { run {
        if (data.isWebLoginValid()) {
            data.app.cookieJar.set("eregister.example.com", "AuthCookie", data.webCookie)
            onSuccess()
        }
        else {
            data.app.cookieJar.clear("eregister.example.com")
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

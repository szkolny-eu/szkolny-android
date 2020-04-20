/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-22
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.login

import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.TextCallbackHandler
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.getUnixDate
import pl.szczodrzynski.edziennik.isNotNullNorEmpty
import pl.szczodrzynski.edziennik.utils.Utils.d

class EdudziennikLoginWeb(val data: DataEdudziennik, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "EdudziennikLoginWeb"
    }

    init { run {
        if (data.isWebLoginValid()) {
            onSuccess()
        }
        else {
            data.app.cookieJar.clear("dziennikel.appspot.com")
            if (data.loginEmail.isNotNullNorEmpty() && data.loginPassword.isNotNullNorEmpty()) {
                loginWithCredentials()
            }
            else {
                data.error(ApiError(TAG, ERROR_LOGIN_DATA_MISSING))
            }
        }
    }}

    private fun loginWithCredentials() {
        d(TAG, "Request: Edudziennik/Login/Web - https://dziennikel.appspot.com/login/?next=/")

        val callback = object : TextCallbackHandler() {
            override fun onSuccess(text: String?, response: Response?) {
                if (text == null || response == null) {
                    data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }

                val url = response.raw().request().url().toString()

                if (!url.contains("Student")) {
                    when {
                        text.contains("Wprowadzono nieprawidłową nazwę użytkownika lub hasło.") -> ERROR_LOGIN_EDUDZIENNIK_WEB_INVALID_LOGIN
                        else -> ERROR_LOGIN_EDUDZIENNIK_WEB_OTHER
                    }.let { errorCode ->
                        data.error(ApiError(TAG, errorCode)
                                .withApiResponse(text)
                                .withResponse(response))
                        return
                    }
                }

                val cookies = data.app.cookieJar.getAll("dziennikel.appspot.com")
                val sessionId = cookies["sessionid"]

                if (sessionId == null) {
                    data.error(ApiError(TAG, ERROR_LOGIN_EDUDZIENNIK_WEB_NO_SESSION_ID)
                            .withResponse(response)
                            .withApiResponse(text))
                    return
                }

                data.webSessionId = sessionId
                data.webSessionIdExpiryTime = response.getUnixDate() + 45 * 60 /* 45 min */
                onSuccess()
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                data.error(ApiError(TAG, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable))
            }
        }

        Request.builder()
                .url("https://dziennikel.appspot.com/login/?next=/")
                .userAgent(EDUDZIENNIK_USER_AGENT)
                .contentType("application/x-www-form-urlencoded")
                .addParameter("email", data.loginEmail)
                .addParameter("password", data.loginPassword)
                .addParameter("auth_method", "password")
                .addParameter("next", "/")
                .post()
                .callback(callback)
                .build()
                .enqueue()
    }
}

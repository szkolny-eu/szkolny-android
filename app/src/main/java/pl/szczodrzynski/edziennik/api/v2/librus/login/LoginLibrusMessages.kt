/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-20.
 */

package pl.szczodrzynski.edziennik.api.v2.librus.login

import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.TextCallbackHandler
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.internal.http.HttpDate
import pl.szczodrzynski.edziennik.api.v2.*
import pl.szczodrzynski.edziennik.api.v2.librus.data.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.currentTimeUnix
import pl.szczodrzynski.edziennik.getUnixDate

class LoginLibrusMessages(val data: DataLibrus, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "LoginLibrusMessages"
    }

    init { run {
        if (data.profile == null) {
            data.error(ApiError(TAG, ERROR_PROFILE_MISSING))
            return@run
        }

        if (data.isMessagesLoginValid()) {
            data.app.cookieJar.saveFromResponse(null, listOf(
                    Cookie.Builder()
                            .name("DZIENNIKSID")
                            .value(data.messagesSessionId!!)
                            .domain("wiadomosci.librus.pl")
                            .secure().httpOnly().build()
            ))
            onSuccess()
        }
        else {
            data.app.cookieJar.clearForDomain("wiadomosci.librus.pl")
            if (data.loginMethods.contains(LOGIN_METHOD_LIBRUS_SYNERGIA)) {
                loginWithSynergia()
            }
            else if (data.apiLogin != null && data.apiPassword != null && false) {
                loginWithCredentials()
            }
            else {
                data.error(ApiError(TAG, ERROR_LOGIN_DATA_MISSING))
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
    private fun loginWithSynergia(url: String = "https://synergia.librus.pl/wiadomosci2") {
        val callback = object : TextCallbackHandler() {
            override fun onSuccess(text: String?, response: Response?) {
                val location = response?.headers()?.get("Location")
                when {
                    location?.contains("MultiDomainLogon") == true -> loginWithSynergia(location)
                    location?.contains("AutoLogon") == true -> {
                        var sessionId = data.app.cookieJar.getCookie("wiadomosci.librus.pl", "DZIENNIKSID")
                        sessionId = sessionId?.replace("-MAINT", "")
                        if (sessionId == null) {
                            data.error(ApiError(TAG, ERROR_LOGIN_LIBRUS_MESSAGES_NO_SESSION_ID)
                                    .withResponse(response)
                                    .withApiResponse(text))
                            return
                        }
                        data.messagesSessionId = sessionId
                        data.messagesSessionIdExpiryTime = response.getUnixDate() + 45 * 60 /* 45min */
                        onSuccess()
                    }

                    text?.contains("eAccessDeny") == true -> data.error(TAG, ERROR_LIBRUS_MESSAGES_ACCESS_DENIED, response, text)
                    text?.contains("stop.png") == true -> data.error(TAG, ERROR_LIBRUS_SYNERGIA_ACCESS_DENIED, response, text)
                }
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                data.error(ApiError(TAG, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable))
            }
        }

        Request.builder()
                .url(url)
                .userAgent(SYNERGIA_USER_AGENT)
                .get()
                .callback(callback)
                .withClient(data.app.httpLazy)
                .build()
                .enqueue()
    }
}
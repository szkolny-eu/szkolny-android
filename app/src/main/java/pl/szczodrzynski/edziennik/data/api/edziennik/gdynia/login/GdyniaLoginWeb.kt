/*
 * Copyright (c) Kacper Ziubryniewicz 2020-5-17
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.gdynia.login

import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.TextCallbackHandler
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.gdynia.DataGdynia
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.get
import pl.szczodrzynski.edziennik.isNotNullNorEmpty
import pl.szczodrzynski.edziennik.md5

class GdyniaLoginWeb(val data: DataGdynia, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "GdyniaLoginWeb"
    }

    init {
        run {
            if (data.isWebLoginValid()) {
                data.app.cookieJar.set("nasze.miasto.gdynia.pl", "sid", data.webSid)
                onSuccess()
            } else {
                data.app.cookieJar.clear("nasze.miasto.gdynia.pl")
                if (data.loginUsername.isNotNullNorEmpty() && data.loginPassword.isNotNullNorEmpty()) {
                    loginWithCredentials()
                } else {
                    data.error(ApiError(TAG, ERROR_LOGIN_DATA_MISSING))
                }
            }
        }
    }

    private fun loginWithCredentials() {
        val checkCallback = object : TextCallbackHandler() {
            override fun onSuccess(text: String?, response: Response?) {
                val cookies = data.app.cookieJar.getAll("nasze.miasto.gdynia.pl")

                data.webSid = cookies["sid"]

                onSuccess()
                return
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                data.error(ApiError(TAG, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable))
            }
        }

        val loginCallback = object : TextCallbackHandler() {
            override fun onSuccess(text: String?, response: Response?) {
                if (text == null || response == null) {
                    data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }

                val error = """<p class="logging_error">(.*?)</p>""".toRegex().find(text)?.get(1)?.trim()

                error?.let { code ->
                    when (code) {
                        "Niepoprawna nazwa użytkownika lub hasło" -> ERROR_LOGIN_GDYNIA_WEB_INVALID_CREDENTIALS
                        else -> ERROR_LOGIN_GDYNIA_WEB_OTHER
                    }.let { errorCode ->
                        data.error(ApiError(TAG, errorCode)
                                .withApiResponse(text)
                                .withResponse(response))
                        return
                    }
                }

                val sid = "sid=(.*?)&".toRegex().find(text)?.get(1)

                if (sid == null) {
                    data.error(ApiError(TAG, ERROR_LOGIN_GDYNIA_WEB_MISSING_SESSION_ID)
                            .withApiResponse(text)
                            .withResponse(response))
                    return
                }

                Request.builder()
                        .url("$GDYNIA_WEB_URL/$GDYNIA_WEB_LOGIN_CHECK")
                        .userAgent(SYSTEM_USER_AGENT)
                        .addParameter("sid", sid)
                        .addParameter("url_back", "")
                        .get()
                        .callback(checkCallback)
                        .build()
                        .enqueue()
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                data.error(ApiError(TAG, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable))
            }
        }

        Request.builder()
                .url("$GDYNIA_WEB_URL/$GDYNIA_WEB_LOGIN")
                .userAgent(SYSTEM_USER_AGENT)
                .addParameter("action", "set")
                .addParameter("user", data.loginUsername)
                .addParameter("login", "Zaloguj się")
                .addParameter("pass_md5", data.loginPassword?.md5())
                .addParameter("url_back", "")
                .post()
                .callback(loginCallback)
                .build()
                .enqueue()
    }
}

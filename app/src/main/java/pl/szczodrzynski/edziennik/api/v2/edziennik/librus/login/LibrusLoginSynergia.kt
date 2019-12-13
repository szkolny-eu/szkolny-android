/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-20.
 */

package pl.szczodrzynski.edziennik.api.v2.edziennik.librus.login

import com.google.gson.JsonObject
import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.TextCallbackHandler
import okhttp3.Cookie
import pl.szczodrzynski.edziennik.api.v2.*
import pl.szczodrzynski.edziennik.api.v2.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.getUnixDate
import pl.szczodrzynski.edziennik.utils.Utils.d
import java.net.HttpURLConnection

class LibrusLoginSynergia(override val data: DataLibrus, val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        private const val TAG = "LoginLibrusSynergia"
    }

    init { run {
        if (data.profile == null) {
            data.error(ApiError(TAG, ERROR_PROFILE_MISSING))
            return@run
        }

        if (data.isSynergiaLoginValid()) {
            data.app.cookieJar.saveFromResponse(null, listOf(
                    Cookie.Builder()
                            .name("DZIENNIKSID")
                            .value(data.synergiaSessionId!!)
                            .domain("synergia.librus.pl")
                            .secure().httpOnly().build()
            ))
            onSuccess()
        }
        else {
            data.app.cookieJar.clearForDomain("synergia.librus.pl")
            if (data.loginMethods.contains(LOGIN_METHOD_LIBRUS_API)) {
                loginWithApi()
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
     * HTML form-based login method. Uses a Synergia login and password.
     */
    private fun loginWithCredentials() {

    }

    /**
     * A login method using the Synergia API (AutoLoginToken endpoint).
     */
    private fun loginWithApi() {
        d(TAG, "Request: Librus/Login/Synergia - $LIBRUS_API_URL/AutoLoginToken")

        val onSuccess = { json: JsonObject ->
            loginWithToken(json.getString("Token"))
        }

        apiGet(TAG, "AutoLoginToken", POST, null, onSuccess)
    }

    private fun loginWithToken(token: String?) {
        if (token == null) {
            data.error(ApiError(TAG, ERROR_LOGIN_LIBRUS_SYNERGIA_NO_TOKEN))
            return
        }

        d(TAG, "Request: Librus/Login/Synergia - " + LIBRUS_SYNERGIA_TOKEN_LOGIN_URL.replace("TOKEN", token) + "/uczen/widok/centrum_powiadomien")

        val callback = object : TextCallbackHandler() {
            override fun onSuccess(json: String?, response: Response?) {
                val location = response?.headers()?.get("Location")
                if (location?.endsWith("przerwa_techniczna") == true) {
                    data.error(ApiError(TAG, ERROR_LIBRUS_SYNERGIA_MAINTENANCE)
                            .withApiResponse(json)
                            .withResponse(response))
                    return
                }

                if (location?.endsWith("centrum_powiadomien") == true) {
                    val sessionId = data.app.cookieJar.getCookie("synergia.librus.pl", "DZIENNIKSID")
                    if (sessionId == null) {
                        data.error(ApiError(TAG, ERROR_LOGIN_LIBRUS_SYNERGIA_NO_SESSION_ID)
                                .withResponse(response)
                                .withApiResponse(json))
                        return
                    }
                    data.synergiaSessionId = sessionId
                    data.synergiaSessionIdExpiryTime = response.getUnixDate() + 45 * 60 /* 45min */
                    onSuccess()
                }
                else {
                    data.error(ApiError(TAG, ERROR_LOGIN_LIBRUS_SYNERGIA_TOKEN_INVALID)
                            .withResponse(response)
                            .withApiResponse(json))
                }
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                data.error(ApiError(TAG, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable))
            }
        }

        data.app.cookieJar.clearForDomain("synergia.librus.pl")
        Request.builder()
                .url(LIBRUS_SYNERGIA_TOKEN_LOGIN_URL.replace("TOKEN", token) + "/uczen/widok/centrum_powiadomien")
                .userAgent(LIBRUS_USER_AGENT)
                .get()
                .allowErrorCode(HttpURLConnection.HTTP_BAD_REQUEST)
                .allowErrorCode(HttpURLConnection.HTTP_FORBIDDEN)
                .allowErrorCode(HttpURLConnection.HTTP_UNAUTHORIZED)
                .callback(callback)
                .withClient(data.app.httpLazy)
                .build()
                .enqueue()
    }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-20.
 */

package pl.szczodrzynski.edziennik.api.v2.librus.login

import com.google.gson.JsonObject
import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.JsonCallbackHandler
import im.wangchao.mhttp.callback.TextCallbackHandler
import okhttp3.Cookie
import pl.szczodrzynski.edziennik.api.v2.*
import pl.szczodrzynski.edziennik.api.v2.librus.data.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.getUnixDate
import java.net.HttpURLConnection

class LibrusLoginSynergia(val data: DataLibrus, val onSuccess: () -> Unit) {
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
        val onSuccess = { json: JsonObject ->
            loginWithToken(json.getString("Token"))
        }

        val callback = object : JsonCallbackHandler() {
            override fun onSuccess(json: JsonObject?, response: Response?) {
                if (json == null && response?.parserErrorBody == null) {
                    data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }
                val error = if (response?.code() == 200) null else
                    json.getString("Code") ?:
                    json.getString("Message") ?:
                    response?.parserErrorBody
                error?.let { code ->
                    when (code) {
                        "TokenIsExpired" -> ERROR_LIBRUS_API_TOKEN_EXPIRED
                        "Insufficient scopes" -> ERROR_LIBRUS_API_INSUFFICIENT_SCOPES
                        "Request is denied" -> ERROR_LIBRUS_API_ACCESS_DENIED
                        "Resource not found" -> ERROR_LIBRUS_API_RESOURCE_NOT_FOUND
                        "NotFound" -> ERROR_LIBRUS_API_DATA_NOT_FOUND
                        "AccessDeny" -> when (json.getString("Message")) {
                            "Student timetable is not public" -> ERROR_LIBRUS_API_TIMETABLE_NOT_PUBLIC
                            else -> ERROR_LIBRUS_API_RESOURCE_ACCESS_DENIED
                        }
                        "LuckyNumberIsNotActive" -> ERROR_LIBRUS_API_LUCKY_NUMBER_NOT_ACTIVE
                        "NotesIsNotActive" -> ERROR_LIBRUS_API_NOTES_NOT_ACTIVE
                        "InvalidRequest" -> ERROR_LIBRUS_API_INVALID_REQUEST_PARAMS
                        "Nieprawidłowy węzeł." -> ERROR_LIBRUS_API_INCORRECT_ENDPOINT
                        else -> ERROR_LIBRUS_API_OTHER
                    }.let { errorCode ->
                        data.error(ApiError(TAG, errorCode)
                                .withApiResponse(json)
                                .withResponse(response))
                        return
                    }
                }

                if (json == null) {
                    data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }

                try {
                    onSuccess(json)
                } catch (e: Exception) {
                    data.error(ApiError(TAG, EXCEPTION_LIBRUS_API_REQUEST)
                            .withResponse(response)
                            .withThrowable(e)
                            .withApiResponse(json))
                }
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                // TODO add hotfix for Classrooms 500
                data.error(ApiError(TAG, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable))
            }
        }

        Request.builder()
                .url("$LIBRUS_API_URL/AutoLoginToken")
                .userAgent(LIBRUS_USER_AGENT)
                .addHeader("Authorization", "Bearer ${data.apiAccessToken}")
                .post()
                .allowErrorCode(HttpURLConnection.HTTP_BAD_REQUEST)
                .allowErrorCode(HttpURLConnection.HTTP_FORBIDDEN)
                .allowErrorCode(HttpURLConnection.HTTP_UNAUTHORIZED)
                .callback(callback)
                .build()
                .enqueue()
    }

    private fun loginWithToken(token: String?) {
        if (token == null) {
            data.error(ApiError(TAG, ERROR_LOGIN_LIBRUS_SYNERGIA_NO_TOKEN))
            return
        }

        val callback = object : TextCallbackHandler() {
            override fun onSuccess(json: String?, response: Response?) {
                val location = response?.headers()?.get("Location")
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
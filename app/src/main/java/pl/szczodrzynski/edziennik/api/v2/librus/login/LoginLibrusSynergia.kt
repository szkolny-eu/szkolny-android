/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-20.
 */

package pl.szczodrzynski.edziennik.api.v2.librus.login

import com.google.gson.JsonObject
import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.JsonCallbackHandler
import im.wangchao.mhttp.callback.TextCallbackHandler
import okhttp3.HttpUrl
import pl.szczodrzynski.edziennik.api.v2.*
import pl.szczodrzynski.edziennik.api.v2.librus.data.DataLibrus
import pl.szczodrzynski.edziennik.currentTimeUnix
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.isNotNullNorEmpty
import java.lang.Exception
import java.net.HttpURLConnection

class LoginLibrusSynergia(val data: DataLibrus, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "LoginLibrusSynergia"
    }

    init { run {
        if (data.profile == null) {
            data.error(TAG, ERROR_PROFILE_MISSING)
            return@run
        }

        if (data.synergiaSessionIdExpiryTime-30 > currentTimeUnix() && data.synergiaSessionId.isNotNullNorEmpty()) {
            onSuccess()
        }
        else {
            if (data.loginMethods.contains(LOGIN_METHOD_LIBRUS_API)) {
                loginWithApi()
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
                    data.error(TAG, ERROR_RESPONSE_EMPTY, response)
                    return
                }
                val error = json.getString("Code") ?: json.getString("Message") ?: response?.parserErrorBody
                error?.let { code ->
                    when (code) {
                        "TokenIsExpired" -> ERROR_LIBRUS_API_TOKEN_EXPIRED
                        "Insufficient scopes" -> ERROR_LIBRUS_API_INSUFFICIENT_SCOPES
                        "Request is denied" -> ERROR_LIBRUS_API_REQUEST_DENIED
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
                        data.error(TAG, errorCode, apiResponse = json, response = response)
                        return
                    }
                }

                if (json == null) {
                    data.error(TAG, ERROR_RESPONSE_EMPTY, response)
                    return
                }

                try {
                    onSuccess(json)
                } catch (e: Exception) {
                    data.error(TAG, EXCEPTION_LIBRUS_API_REQUEST, response, e, json)
                }
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                // TODO add hotfix for Classrooms 500
                data.error(TAG, ERROR_REQUEST_FAILURE, response, throwable)
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
            data.error(TAG, ERROR_LOGIN_LIBRUS_SYNERGIA_NO_TOKEN)
            return
        }

        val callback = object : TextCallbackHandler() {
            override fun onSuccess(json: String?, response: Response?) {
                val location = response?.headers()?.get("Location")
                if (location?.endsWith("centrum_powiadomien") == true) {
                    val cookieList = data.app.cookieJar.loadForRequest(HttpUrl.get("https://synergia.librus.pl"))
                    var sessionId: String? = null
                    for (cookie in cookieList) {
                        if (cookie.name().equals("DZIENNIKSID", ignoreCase = true)) {
                            sessionId = cookie.value()
                        }
                    }
                    if (sessionId == null) {
                        data.error(TAG, ERROR_LOGIN_LIBRUS_SYNERGIA_NO_SESSION_ID, response, json)
                        return
                    }
                    data.synergiaSessionId = sessionId
                    data.synergiaSessionIdExpiryTime = currentTimeUnix() + 3600 /* 1h */
                    onSuccess()
                }
                else {
                    data.error(TAG, ERROR_LOGIN_LIBRUS_SYNERGIA_TOKEN_INVALID, response, json)
                }
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                data.error(TAG, ERROR_REQUEST_FAILURE, response, throwable)
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
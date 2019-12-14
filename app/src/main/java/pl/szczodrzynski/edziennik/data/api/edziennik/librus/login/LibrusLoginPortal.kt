package pl.szczodrzynski.edziennik.data.api.edziennik.librus.login

import android.util.Pair
import com.google.gson.JsonObject
import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.body.MediaTypeUtils
import im.wangchao.mhttp.callback.JsonCallbackHandler
import im.wangchao.mhttp.callback.TextCallbackHandler
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.getInt
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.getUnixDate
import pl.szczodrzynski.edziennik.utils.Utils.d
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED
import java.util.*
import java.util.regex.Pattern

class LibrusLoginPortal(val data: DataLibrus, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "LoginLibrusPortal"
    }

    init { run {
        if (data.loginStore.mode != LOGIN_MODE_LIBRUS_EMAIL) {
            data.error(ApiError(TAG, ERROR_INVALID_LOGIN_MODE))
            return@run
        }
        if (data.portalEmail == null || data.portalPassword == null) {
            data.error(ApiError(TAG, ERROR_LOGIN_DATA_MISSING))
            return@run
        }

        // succeed having a non-expired access token and a refresh token
        if (data.isPortalLoginValid()) {
            onSuccess()
        }
        else if (data.portalRefreshToken != null) {
            data.app.cookieJar.clearForDomain("portal.librus.pl")
            accessToken(null, data.portalRefreshToken)
        }
        else {
            data.app.cookieJar.clearForDomain("portal.librus.pl")
            authorize(if (data.fakeLogin) FAKE_LIBRUS_AUTHORIZE else LIBRUS_AUTHORIZE_URL)
        }
    }}

    private fun authorize(url: String?) {
        d(TAG, "Request: Librus/Login/Portal - $url")

        Request.builder()
                .url(url)
                .userAgent(LIBRUS_USER_AGENT)
                .withClient(data.app.httpLazy)
                .callback(object : TextCallbackHandler() {
                    override fun onSuccess(json: String, response: Response) {
                        val location = response.headers().get("Location")
                        if (location != null) {
                            val authMatcher = Pattern.compile("http://localhost/bar\\?code=([A-z0-9]+?)$", Pattern.DOTALL or Pattern.MULTILINE).matcher(location)
                            if (authMatcher.find()) {
                                accessToken(authMatcher.group(1), null)
                            } else {
                                authorize(location)
                            }
                        } else {
                            val csrfMatcher = Pattern.compile("name=\"csrf-token\" content=\"([A-z0-9=+/\\-_]+?)\"", Pattern.DOTALL).matcher(json)
                            if (csrfMatcher.find()) {
                                login(csrfMatcher.group(1))
                            } else {
                                data.error(ApiError(TAG, ERROR_LOGIN_LIBRUS_PORTAL_CSRF_MISSING)
                                        .withResponse(response)
                                        .withApiResponse(json))
                            }
                        }
                    }

                    override fun onFailure(response: Response, throwable: Throwable) {
                        data.error(ApiError(TAG, ERROR_REQUEST_FAILURE)
                                .withResponse(response)
                                .withThrowable(throwable))
                    }
                })
                .build()
                .enqueue()
    }

    private fun login(csrfToken: String) {
        d(TAG, "Request: Librus/Login/Portal - ${if (data.fakeLogin) FAKE_LIBRUS_LOGIN else LIBRUS_LOGIN_URL}")

        Request.builder()
                .url(if (data.fakeLogin) FAKE_LIBRUS_LOGIN else LIBRUS_LOGIN_URL)
                .userAgent(LIBRUS_USER_AGENT)
                .addParameter("email", data.portalEmail)
                .addParameter("password", data.portalPassword)
                .addHeader("X-CSRF-TOKEN", csrfToken)
                .contentType(MediaTypeUtils.APPLICATION_JSON)
                .post()
                .callback(object : JsonCallbackHandler() {
                    override fun onSuccess(json: JsonObject?, response: Response) {
                        val location = response.headers()?.get("Location")
                        if (location == "http://localhost/bar?command=close") {
                            data.error(ApiError(TAG, ERROR_LIBRUS_PORTAL_MAINTENANCE)
                                    .withApiResponse(json)
                                    .withResponse(response))
                            return
                        }

                        if (json == null) {
                            if (response.parserErrorBody?.contains("wciąż nieaktywne") == true) {
                                data.error(ApiError(TAG, ERROR_LOGIN_LIBRUS_PORTAL_NOT_ACTIVATED)
                                        .withResponse(response))
                                return
                            }
                            data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                                    .withResponse(response))
                            return
                        }
                        if (json.get("errors") != null) {
                            data.error(ApiError(TAG, ERROR_LOGIN_LIBRUS_PORTAL_ACTION_ERROR)
                                    .withResponse(response)
                                    .withApiResponse(json))
                            return
                        }
                        authorize(json.getString("redirect", LIBRUS_AUTHORIZE_URL))
                    }

                    override fun onFailure(response: Response, throwable: Throwable) {
                        if (response.code() == 403 || response.code() == 401) {
                            data.error(ApiError(TAG, ERROR_LOGIN_LIBRUS_PORTAL_INVALID_LOGIN)
                                    .withResponse(response)
                                    .withThrowable(throwable))
                            return
                        }
                        data.error(ApiError(TAG, ERROR_REQUEST_FAILURE)
                                .withResponse(response)
                                .withThrowable(throwable))
                    }
                })
                .build()
                .enqueue()
    }

    private var refreshTokenFailed = false
    private fun accessToken(code: String?, refreshToken: String?) {
        d(TAG, "Request: Librus/Login/Portal - ${if (data.fakeLogin) FAKE_LIBRUS_TOKEN else LIBRUS_TOKEN_URL}")

        val onSuccess = { json: JsonObject, response: Response? ->
            data.portalAccessToken = json.getString("access_token")
            data.portalRefreshToken = json.getString("refresh_token")
            data.portalTokenExpiryTime = response.getUnixDate() + json.getInt("expires_in", 86400)
            onSuccess()
        }

        val callback = object : JsonCallbackHandler() {
            override fun onSuccess(json: JsonObject?, response: Response?) {
                if (json == null) {
                    data.error(TAG, ERROR_RESPONSE_EMPTY, response)
                    return
                }
                val error = if (response?.code() == 200) null else
                    json.getString("hint")
                error?.let { code ->
                    when (code) {
                        "Authorization code has expired" -> ERROR_LOGIN_LIBRUS_PORTAL_CODE_EXPIRED
                        "Authorization code has been revoked" -> ERROR_LOGIN_LIBRUS_PORTAL_CODE_REVOKED
                        "Cannot decrypt the refresh token" -> ERROR_LOGIN_LIBRUS_PORTAL_REFRESH_INVALID
                        "Token has been revoked" -> ERROR_LOGIN_LIBRUS_PORTAL_REFRESH_REVOKED
                        "Check the `client_id` parameter" -> ERROR_LOGIN_LIBRUS_PORTAL_NO_CLIENT_ID
                        "Check the `code` parameter" -> ERROR_LOGIN_LIBRUS_PORTAL_NO_CODE
                        "Check the `refresh_token` parameter" -> ERROR_LOGIN_LIBRUS_PORTAL_NO_REFRESH
                        "Check the `redirect_uri` parameter" -> ERROR_LOGIN_LIBRUS_PORTAL_NO_REDIRECT
                        else -> when (json.getString("error")) {
                            "unsupported_grant_type" -> ERROR_LOGIN_LIBRUS_PORTAL_UNSUPPORTED_GRANT
                            "invalid_client" -> ERROR_LOGIN_LIBRUS_PORTAL_INVALID_CLIENT_ID
                            else -> ERROR_LOGIN_LIBRUS_PORTAL_OTHER
                        }
                    }.let { errorCode ->
                        data.error(ApiError(TAG, errorCode)
                                .withApiResponse(json)
                                .withResponse(response))
                        return
                    }
                }

                try {
                    onSuccess(json, response)
                } catch (e: NullPointerException) {
                    data.error(ApiError(TAG, EXCEPTION_LOGIN_LIBRUS_PORTAL_TOKEN)
                            .withResponse(response)
                            .withThrowable(e)
                            .withApiResponse(json))
                }
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                data.error(ApiError(TAG, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable))
            }
        }

        val params = ArrayList<Pair<String, Any>>()
        params.add(Pair("client_id", LIBRUS_CLIENT_ID))
        if (code != null) {
            params.add(Pair("grant_type", "authorization_code"))
            params.add(Pair("code", code))
            params.add(Pair("redirect_uri", LIBRUS_REDIRECT_URL))
        } else if (refreshToken != null) {
            params.add(Pair("grant_type", "refresh_token"))
            params.add(Pair("refresh_token", refreshToken))
        }

        Request.builder()
                .url(if (data.fakeLogin) FAKE_LIBRUS_TOKEN else LIBRUS_TOKEN_URL)
                .userAgent(LIBRUS_USER_AGENT)
                .addParams(params)
                .post()
                .allowErrorCode(HTTP_UNAUTHORIZED)
                .callback(callback)
                .build()
                .enqueue()
    }
}

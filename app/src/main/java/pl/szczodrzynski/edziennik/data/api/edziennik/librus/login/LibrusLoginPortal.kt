package pl.szczodrzynski.edziennik.data.api.edziennik.librus.login

import android.util.Pair
import com.google.gson.JsonObject
import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.body.MediaTypeUtils
import im.wangchao.mhttp.callback.JsonCallbackHandler
import im.wangchao.mhttp.callback.TextCallbackHandler
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.events.UserActionRequiredEvent
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.enums.LoginMode
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.utils.Utils.d
import java.net.HttpURLConnection.*
import java.util.*
import java.util.regex.Pattern

class LibrusLoginPortal(val data: DataLibrus, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "LoginLibrusPortal"
    }

    init { run {
        if (data.loginStore.mode != LoginMode.LIBRUS_EMAIL) {
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
            if (data.fakeLogin) {
                data.app.cookieJar.clear("librus.szkolny.eu")
            }
            else {
                data.app.cookieJar.clear("portal.librus.pl")
            }
            accessToken(null, data.portalRefreshToken)
        }
        else {
            if (data.fakeLogin) {
                data.app.cookieJar.clear("librus.szkolny.eu")
            }
            else {
                data.app.cookieJar.clear("portal.librus.pl")
            }
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
                    override fun onSuccess(text: String, response: Response) {
                        val location = response.headers().get("Location")
                        if (location != null) {
                            val authMatcher = Pattern.compile("$LIBRUS_REDIRECT_URL\\?code=([A-z0-9]+?)$", Pattern.DOTALL or Pattern.MULTILINE).matcher(location)
                            when {
                                authMatcher.find() -> {
                                    accessToken(authMatcher.group(1), null)
                                }
                                location.contains("rejected_client") -> {
                                    data.error(ApiError(TAG, ERROR_LOGIN_LIBRUS_PORTAL_INVALID_CLIENT_ID)
                                            .withResponse(response)
                                            .withApiResponse("Location: $location\n$text"))
                                }
                                else -> {
                                    authorize(location)
                                }
                            }
                        } else {
                            val csrfMatcher = Pattern.compile("name=\"csrf-token\" content=\"([A-z0-9=+/\\-_]+?)\"", Pattern.DOTALL).matcher(text)
                            if (csrfMatcher.find()) {
                                login(csrfMatcher.group(1) ?: "")
                            } else {
                                data.error(ApiError(TAG, ERROR_LOGIN_LIBRUS_PORTAL_CSRF_MISSING)
                                        .withResponse(response)
                                        .withApiResponse(text))
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

        val recaptchaCode = data.arguments?.getString("recaptchaCode") ?: data.loginStore.getLoginData("recaptchaCode", null)
        val recaptchaTime = data.arguments?.getLong("recaptchaTime") ?: data.loginStore.getLoginData("recaptchaTime", 0L)
        data.loginStore.removeLoginData("recaptchaCode")
        data.loginStore.removeLoginData("recaptchaTime")

        Request.builder()
                .url(if (data.fakeLogin) FAKE_LIBRUS_LOGIN else LIBRUS_LOGIN_URL)
                .userAgent(LIBRUS_USER_AGENT)
                .addParameter("email", data.portalEmail)
                .addParameter("password", data.portalPassword)
                .also {
                    if (recaptchaCode != null && System.currentTimeMillis() - recaptchaTime < 2*60*1000 /* 2 minutes */)
                        it.addParameter("g-recaptcha-response", recaptchaCode)
                }
                .addHeader("X-CSRF-TOKEN", csrfToken)
                .allowErrorCode(HTTP_BAD_REQUEST)
                .allowErrorCode(HTTP_FORBIDDEN)
                .contentType(MediaTypeUtils.APPLICATION_JSON)
                .post()
                .callback(object : TextCallbackHandler() {
                    override fun onSuccess(text: String?, response: Response) {
                        val location = response.headers()?.get("Location")
                        if (location == "$LIBRUS_REDIRECT_URL?command=close") {
                            data.error(ApiError(TAG, ERROR_LIBRUS_PORTAL_MAINTENANCE)
                                    .withApiResponse(text)
                                    .withResponse(response))
                            return
                        }
                        if (text == null) {
                            data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                                    .withResponse(response))
                            return
                        }
                        val error = if (response.code() == 200 && text.contains("librus_account_settings_main")) null else
                            text

                        if (error?.contains("robotem") == true) {
                            data.requireUserAction(
                                type = UserActionRequiredEvent.Type.RECAPTCHA,
                                params = Bundle(
                                    "siteKey" to LIBRUS_PORTAL_RECAPTCHA_KEY,
                                    "referer" to LIBRUS_PORTAL_RECAPTCHA_REFERER,
                                ),
                                errorText = R.string.notification_user_action_required_captcha_librus,
                            )
                            return
                        }

                        error?.let { code ->
                            when {
                                code.contains("Sesja logowania wygasła") -> ERROR_LOGIN_LIBRUS_PORTAL_CSRF_EXPIRED
                                code.contains("Upewnij się, że nie") -> ERROR_LOGIN_LIBRUS_PORTAL_INVALID_LOGIN
                                code.contains("Podany adres e-mail jest nieprawidłowy.") -> ERROR_LOGIN_LIBRUS_PORTAL_INVALID_LOGIN
                                else -> ERROR_LOGIN_LIBRUS_PORTAL_ACTION_ERROR
                            }.let { errorCode ->
                                data.error(ApiError(TAG, errorCode)
                                    .withApiResponse(text)
                                    .withResponse(response))
                                return
                            }
                        }
                        authorize(LIBRUS_AUTHORIZE_URL)
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
                    json.getString("hint") ?: json.getString("error")
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
                        "unsupported_grant_type" -> ERROR_LOGIN_LIBRUS_PORTAL_UNSUPPORTED_GRANT
                        "invalid_client" -> ERROR_LOGIN_LIBRUS_PORTAL_INVALID_CLIENT_ID
                        else -> ERROR_LOGIN_LIBRUS_PORTAL_OTHER
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

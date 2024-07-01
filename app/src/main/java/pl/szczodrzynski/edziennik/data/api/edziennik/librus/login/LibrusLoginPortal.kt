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
import pl.szczodrzynski.edziennik.data.enums.LoginMode
import pl.szczodrzynski.edziennik.ext.*
import timber.log.Timber
import java.net.HttpURLConnection.*
import java.util.*
import java.util.regex.Pattern

class LibrusLoginPortal(val data: DataLibrus, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "LoginLibrusPortal"
    }

    // loop failsafe
    private var loginPerformed = false

    init { run {
        if (data.loginStore.mode != LoginMode.LIBRUS_EMAIL) {
            data.error(ApiError(TAG, ERROR_INVALID_LOGIN_MODE))
            return@run
        }
        if (data.portalEmail == null || data.portalPassword == null) {
            data.error(ApiError(TAG, ERROR_LOGIN_DATA_MISSING))
            return@run
        }
        loginPerformed = false

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

    private fun authorize(url: String, referer: String? = null) {
        Timber.d("Request: Librus/Login/Portal - $url")

        Request.builder()
                .url(url)
                .userAgent(LIBRUS_USER_AGENT)
                .also {
                    if (referer != null)
                        it.addHeader("Referer", referer)
                }
                .addHeader("X-Requested-With", LIBRUS_HEADER)
                .withClient(data.app.httpLazy)
                .callback(object : TextCallbackHandler() {
                    override fun onSuccess(text: String, response: Response) {
                        val location = response.headers().get("Location")
                        if (location != null) {
                            val authMatcher = Pattern.compile("$LIBRUS_REDIRECT_URL\\?code=([^&?]+)", Pattern.DOTALL or Pattern.MULTILINE).matcher(location)
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
                            return
                        }

                        if (checkError(text, response))
                            return

                        var loginUrl = if (data.fakeLogin) FAKE_LIBRUS_LOGIN else LIBRUS_LOGIN_URL
                        val csrfToken = Regexes.HTML_CSRF_TOKEN.find(text)?.get(1) ?: ""

                        for (match in Regexes.HTML_FORM_ACTION.findAll(text)) {
                            val form = match.value.lowercase()
                            if ("login" in form && "post" in form) {
                                loginUrl = match[1]
                            }
                        }

                        val params = mutableMapOf<String, String>()
                        for (match in Regexes.HTML_INPUT_HIDDEN.findAll(text)) {
                            val input = match.value
                            val name = Regexes.HTML_INPUT_NAME.find(input)?.get(1) ?: continue
                            val value = Regexes.HTML_INPUT_VALUE.find(input)?.get(1) ?: continue
                            params[name] = value
                        }

                        login(url = loginUrl, referer = url, csrfToken, params)
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

    private fun checkError(text: String, response: Response): Boolean {
        when {
            text.contains("librus_account_settings_main") -> return false
            text.contains("Sesja logowania wygasła") -> ERROR_LOGIN_LIBRUS_PORTAL_CSRF_EXPIRED
            text.contains("Upewnij się, że nie") -> ERROR_LOGIN_LIBRUS_PORTAL_INVALID_LOGIN
            text.contains("Podany adres e-mail jest nieprawidłowy.") -> ERROR_LOGIN_LIBRUS_PORTAL_INVALID_LOGIN
            else -> null // no error for now
        }?.let { errorCode ->
            data.error(ApiError(TAG, errorCode)
                .withApiResponse(text)
                .withResponse(response))
            return true
        }

        if ("robotem" in text || "g-recaptcha" in text || "captchaValidate" in text) {
            val siteKey = Regexes.HTML_RECAPTCHA_KEY.find(text)?.get(1)
            if (siteKey == null) {
                data.error(ApiError(TAG, ERROR_LOGIN_LIBRUS_PORTAL_ACTION_ERROR)
                    .withApiResponse(text)
                    .withResponse(response))
                return true
            }
            data.requireUserAction(
                type = UserActionRequiredEvent.Type.RECAPTCHA,
                params = Bundle(
                    "siteKey" to siteKey,
                    "referer" to response.request().url().toString(),
                    "userAgent" to LIBRUS_USER_AGENT,
                ),
                errorText = R.string.notification_user_action_required_captcha_librus,
            )
            return true
        }
        return false
    }

    private fun login(
        url: String,
        referer: String,
        csrfToken: String?,
        params: Map<String, String>,
    ) {
        if (loginPerformed) {
            data.error(ApiError(TAG, ERROR_LOGIN_LIBRUS_PORTAL_ACTION_ERROR))
            return
        }

        Timber.d("Request: Librus/Login/Portal - $url")

        val recaptchaCode = data.arguments?.getString("recaptchaCode") ?: data.loginStore.getLoginData("recaptchaCode", null)
        val recaptchaTime = data.arguments?.getLong("recaptchaTime") ?: data.loginStore.getLoginData("recaptchaTime", 0L)
        data.loginStore.removeLoginData("recaptchaCode")
        data.loginStore.removeLoginData("recaptchaTime")

        Request.builder()
                .url(if (data.fakeLogin) FAKE_LIBRUS_LOGIN else LIBRUS_LOGIN_URL)
                .userAgent(LIBRUS_USER_AGENT)
                .addHeader("X-Requested-With", LIBRUS_HEADER)
                .addHeader("Referer", referer)
                .withClient(data.app.httpLazy)
                .addParameter("email", data.portalEmail)
                .addParameter("password", data.portalPassword)
                .also {
                    if (recaptchaCode != null && System.currentTimeMillis() - recaptchaTime < 2*60*1000 /* 2 minutes */)
                        it.addParameter("g-recaptcha-response", recaptchaCode)
                    if (csrfToken != null)
                        it.addHeader("X-CSRF-TOKEN", csrfToken)
                    for ((key, value) in params) {
                        it.addParameter(key, value)
                    }
                }
                .contentType(MediaTypeUtils.APPLICATION_FORM)
                .post()
                .callback(object : TextCallbackHandler() {
                    override fun onSuccess(text: String?, response: Response) {
                        loginPerformed = true
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

                        authorize(
                            url = location
                                ?: if (data.fakeLogin)
                                    FAKE_LIBRUS_AUTHORIZE
                                else
                                    LIBRUS_AUTHORIZE_URL,
                            referer = referer,
                        )
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
        Timber.d("Request: Librus/Login/Portal - ${if (data.fakeLogin) FAKE_LIBRUS_TOKEN else LIBRUS_TOKEN_URL}")

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

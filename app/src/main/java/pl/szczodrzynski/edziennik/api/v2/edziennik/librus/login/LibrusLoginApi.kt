/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-20.
 */

package pl.szczodrzynski.edziennik.api.v2.edziennik.librus.login

import com.google.gson.JsonObject
import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.body.MediaTypeUtils
import im.wangchao.mhttp.callback.JsonCallbackHandler
import pl.szczodrzynski.edziennik.api.v2.*
import pl.szczodrzynski.edziennik.api.v2.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.getInt
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.getUnixDate
import pl.szczodrzynski.edziennik.utils.Utils.d
import java.net.HttpURLConnection.*

class LibrusLoginApi {
    companion object {
        private const val TAG = "LoginLibrusApi"
    }

    private lateinit var data: DataLibrus
    private lateinit var onSuccess: () -> Unit

    /* do NOT move this to primary constructor */
    constructor(data: DataLibrus, onSuccess: () -> Unit) {
        this.data = data
        this.onSuccess = onSuccess

        if (data.loginStore.mode == LOGIN_MODE_LIBRUS_EMAIL && data.profile == null) {
            data.error(ApiError(TAG, ERROR_PROFILE_MISSING))
            return
        }

        if (data.isApiLoginValid()) {
            onSuccess()
        }
        else {
            when (data.loginStore.mode) {
                LOGIN_MODE_LIBRUS_EMAIL -> loginWithPortal()
                LOGIN_MODE_LIBRUS_SYNERGIA -> loginWithSynergia()
                LOGIN_MODE_LIBRUS_JST -> loginWithJst()
                else -> {
                    data.error(ApiError(TAG, ERROR_INVALID_LOGIN_MODE))
                }
            }
        }
    }

    private fun loginWithPortal() {
        if (!data.loginMethods.contains(LOGIN_METHOD_LIBRUS_PORTAL)) {
            data.error(ApiError(TAG, ERROR_LOGIN_METHOD_NOT_SATISFIED))
            return
        }
        SynergiaTokenExtractor(data) {
            onSuccess()
        }
    }

    private fun copyFromLoginStore() {
        data.loginStore.data?.apply {
            if (has("accountLogin")) {
                data.apiLogin = getString("accountLogin")
                remove("accountLogin")
            }
            if (has("accountPassword")) {
                data.apiPassword = getString("accountPassword")
                remove("accountPassword")
            }
            if (has("accountCode")) {
                data.apiCode = getString("accountCode")
                remove("accountCode")
            }
            if (has("accountPin")) {
                data.apiPin = getString("accountPin")
                remove("accountPin")
            }
        }
    }

    private fun loginWithSynergia() {
        copyFromLoginStore()
        if (data.apiRefreshToken != null) {
            // refresh a Synergia token
            synergiaRefreshToken()
        }
        else if (data.apiLogin != null && data.apiPassword != null) {
            synergiaGetToken()
        }
        else {
            // cannot log in: token expired, no login data present
            data.error(ApiError(TAG, ERROR_LOGIN_DATA_MISSING))
        }
    }

    private fun loginWithJst() {
        copyFromLoginStore()

        if (data.apiRefreshToken != null) {
            // refresh a JST token
            jstRefreshToken()
        }
        else if (data.apiCode != null && data.apiPin != null) {
            // get a JST token from Code and PIN
            jstGetToken()
        }
        else {
            // cannot log in: token expired, no login data present
            data.error(ApiError(TAG, ERROR_LOGIN_DATA_MISSING))
        }
    }

    private val tokenCallback = object : JsonCallbackHandler() {
        override fun onSuccess(json: JsonObject?, response: Response?) {
            if (response?.code() == HTTP_UNAVAILABLE) {
                data.error(ApiError(TAG, ERROR_LIBRUS_API_MAINTENANCE)
                        .withApiResponse(json)
                        .withResponse(response))
                return
            }

            if (json == null) {
                data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                        .withResponse(response))
                return
            }
            if (response?.code() != 200) json.getString("error")?.let { error ->
                when (error) {
                    "librus_captcha_needed" -> ERROR_LOGIN_LIBRUS_API_CAPTCHA_NEEDED
                    "connection_problems" -> ERROR_LOGIN_LIBRUS_API_CONNECTION_PROBLEMS
                    "invalid_client" -> ERROR_LOGIN_LIBRUS_API_INVALID_CLIENT
                    "librus_reg_accept_needed" -> ERROR_LOGIN_LIBRUS_API_REG_ACCEPT_NEEDED
                    "librus_change_password_error" -> ERROR_LOGIN_LIBRUS_API_CHANGE_PASSWORD_ERROR
                    "librus_password_change_required" -> ERROR_LOGIN_LIBRUS_API_PASSWORD_CHANGE_REQUIRED
                    "invalid_grant" -> ERROR_LOGIN_LIBRUS_API_INVALID_LOGIN
                    else -> ERROR_LOGIN_LIBRUS_API_OTHER
                }.let { errorCode ->
                    data.error(ApiError(TAG, errorCode)
                            .withApiResponse(json)
                            .withResponse(response))
                    return
                }
            }

            try {
                data.apiAccessToken = json.getString("access_token")
                data.apiRefreshToken = json.getString("refresh_token")
                data.apiTokenExpiryTime = response.getUnixDate() + json.getInt("expires_in", 86400)
                onSuccess()
            } catch (e: NullPointerException) {
                data.error(ApiError(TAG, EXCEPTION_LOGIN_LIBRUS_API_TOKEN)
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

    private fun synergiaGetToken() {
        d(TAG, "Request: Librus/Login/Api - $LIBRUS_API_TOKEN_URL")

        Request.builder()
                .url(LIBRUS_API_TOKEN_URL)
                .userAgent(LIBRUS_USER_AGENT)
                .addParameter("grant_type", "password")
                .addParameter("username", data.apiLogin)
                .addParameter("password", data.apiPassword)
                .addParameter("librus_long_term_token", "1")
                .addParameter("librus_rules_accepted", "1")
                .addHeader("Authorization", "Basic $LIBRUS_API_AUTHORIZATION")
                .contentType(MediaTypeUtils.APPLICATION_FORM)
                .post()
                .allowErrorCode(HTTP_BAD_REQUEST)
                .allowErrorCode(HTTP_UNAUTHORIZED)
                .allowErrorCode(HTTP_UNAVAILABLE)
                .callback(tokenCallback)
                .build()
                .enqueue()
    }
    private fun synergiaRefreshToken() {
        d(TAG, "Request: Librus/Login/Api - $LIBRUS_API_TOKEN_URL")

        Request.builder()
                .url(LIBRUS_API_TOKEN_URL)
                .userAgent(LIBRUS_USER_AGENT)
                .addParameter("grant_type", "refresh_token")
                .addParameter("refresh_token", data.apiRefreshToken)
                .addParameter("librus_long_term_token", "1")
                .addParameter("librus_rules_accepted", "1")
                .addHeader("Authorization", "Basic $LIBRUS_API_AUTHORIZATION")
                .contentType(MediaTypeUtils.APPLICATION_FORM)
                .post()
                .allowErrorCode(HTTP_BAD_REQUEST)
                .allowErrorCode(HTTP_UNAUTHORIZED)
                .callback(tokenCallback)
                .build()
                .enqueue()
    }
    private fun jstGetToken() {
        d(TAG, "Request: Librus/Login/Api - $LIBRUS_API_TOKEN_JST_URL")

        Request.builder()
                .url(LIBRUS_API_TOKEN_JST_URL)
                .userAgent(LIBRUS_USER_AGENT)
                .addParameter("grant_type", "implicit_grant")
                .addParameter("client_id", LIBRUS_API_CLIENT_ID_JST)
                .addParameter("secret", LIBRUS_API_SECRET_JST)
                .addParameter("code", data.apiCode)
                .addParameter("pin", data.apiPin)
                .addParameter("librus_rules_accepted", "1")
                .addParameter("librus_mobile_rules_accepted", "1")
                .addParameter("librus_long_term_token", "1")
                .contentType(MediaTypeUtils.APPLICATION_FORM)
                .post()
                .allowErrorCode(HTTP_BAD_REQUEST)
                .allowErrorCode(HTTP_UNAUTHORIZED)
                .callback(tokenCallback)
                .build()
                .enqueue()
    }
    private fun jstRefreshToken() {
        d(TAG, "Request: Librus/Login/Api - $LIBRUS_API_TOKEN_JST_URL")

        Request.builder()
                .url(LIBRUS_API_TOKEN_JST_URL)
                .userAgent(LIBRUS_USER_AGENT)
                .addParameter("grant_type", "refresh_token")
                .addParameter("client_id", LIBRUS_API_CLIENT_ID_JST)
                .addParameter("refresh_token", data.apiRefreshToken)
                .addParameter("librus_long_term_token", "1")
                .addParameter("mobile_app_accept_rules", "1")
                .addParameter("synergy_accept_rules", "1")
                .contentType(MediaTypeUtils.APPLICATION_FORM)
                .post()
                .allowErrorCode(HTTP_BAD_REQUEST)
                .allowErrorCode(HTTP_UNAUTHORIZED)
                .callback(tokenCallback)
                .build()
                .enqueue()
    }
}

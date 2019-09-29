package pl.szczodrzynski.edziennik.api.v2.librus.login

import com.google.gson.JsonNull
import com.google.gson.JsonObject
import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.JsonCallbackHandler
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.AppError
import pl.szczodrzynski.edziennik.api.AppError.*
import pl.szczodrzynski.edziennik.api.v2.*
import pl.szczodrzynski.edziennik.api.v2.librus.data.DataLibrus
import pl.szczodrzynski.edziennik.utils.Utils.d
import java.net.HttpURLConnection.*

class SynergiaTokenExtractor(val data: DataLibrus, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "SynergiaTokenExtractor"
    }

    init { run {
        if (data.loginStore.mode != LOGIN_MODE_LIBRUS_EMAIL) {
            data.error(TAG, ERROR_INVALID_LOGIN_MODE)
            return@run
        }
        if (data.profile == null) {
            data.error(TAG, ERROR_PROFILE_MISSING)
            return@run
        }

        if (data.apiTokenExpiryTime-30 > currentTimeUnix() && data.apiAccessToken.isNotNullNorEmpty()) {
            onSuccess()
        }
        else {
            if (!synergiaAccount()) {
                data.error(TAG, ERROR_LOGIN_DATA_MISSING)
            }
        }
    }}

    /**
     * Get an Api token from the Portal account, using Portal API.
     * If necessary, refreshes the token.
     */
    private fun synergiaAccount(): Boolean {
        val accountLogin = data.apiLogin ?: return false
        val accessToken = data.portalAccessToken ?: return false

        val onSuccess = { json: JsonObject, response: Response? ->
            // synergiaAccount is executed when a synergia token needs a refresh
            val accountId = json.getInt("id")
            val accountToken = json.getString("accessToken")
            if (accountId == null || accountToken == null) {
                data.error(TAG, ERROR_LOGIN_LIBRUS_PORTAL_SYNERGIA_TOKEN_MISSING, response, apiResponse = json)
            }
            else {
                data.apiAccessToken = accountToken
                data.apiTokenExpiryTime = response.getUnixDate() + 6 * 60 * 60

                // TODO remove this
                data.profile?.studentNameLong = json.getString("studentName")
                val nameParts = json.getString("studentName")?.split(" ")?.toTypedArray()
                data.profile?.studentNameShort = nameParts?.get(0) + " " + nameParts?.get(1)?.get(0)

                onSuccess()
            }
        }

        val callback = object : JsonCallbackHandler() {
            override fun onSuccess(json: JsonObject?, response: Response?) {
                if (json == null) {
                    data.error(TAG, ERROR_RESPONSE_EMPTY, response)
                    return
                }
                val error = if (response?.code() == 200) null else
                    json.getString("reason") ?:
                    json.getString("message") ?:
                    json.getString("hint") ?:
                    json.getString("Code")
                error?.let { code ->
                    when (code) {
                        "requires_an_action" -> ERROR_LIBRUS_PORTAL_SYNERGIA_DISCONNECTED
                        "Access token is invalid" -> ERROR_LIBRUS_PORTAL_TOKEN_EXPIRED
                        "ApiDisabled" -> ERROR_LIBRUS_PORTAL_API_DISABLED
                        "Account not found" -> ERROR_LIBRUS_PORTAL_SYNERGIA_NOT_FOUND
                        else -> ERROR_LIBRUS_PORTAL_OTHER
                    }.let { errorCode ->
                        data.error(TAG, errorCode, apiResponse = json, response = response)
                        return
                    }
                }
                if (response?.code() == HTTP_OK) {
                    try {
                        onSuccess(json, response)
                    } catch (e: NullPointerException) {
                        e.printStackTrace()
                        data.error(TAG, EXCEPTION_LIBRUS_PORTAL_SYNERGIA_TOKEN, response, e, json)
                    }

                } else {
                    data.error(TAG, ERROR_REQUEST_FAILURE, response, apiResponse = json)
                }
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                data.error(TAG, ERROR_REQUEST_FAILURE, response, throwable)
            }
        }

        Request.builder()
                .url(LIBRUS_ACCOUNT_URL + accountLogin)
                .userAgent(LIBRUS_USER_AGENT)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .allowErrorCode(HTTP_NOT_FOUND)
                .allowErrorCode(HTTP_FORBIDDEN)
                .allowErrorCode(HTTP_UNAUTHORIZED)
                .allowErrorCode(HTTP_BAD_REQUEST)
                .allowErrorCode(HTTP_GONE)
                .callback(callback)
                .build()
                .enqueue()
        return true
    }
}
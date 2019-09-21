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
        private const val TAG = "librus.SynergiaToken"
    }

    init { run {
        if (data.loginStore.mode != LOGIN_MODE_LIBRUS_EMAIL) {
            data.callback.onError(null, AppError(TAG, 23, CODE_INVALID_LOGIN_MODE))
            return@run
        }
        if (data.profile == null) {
            data.callback.onError(null, AppError(TAG, 28, CODE_LIBRUS_PROFILE_NULL))
            return@run
        }

        if (data.apiTokenExpiryTime-30 > currentTimeUnix() && data.apiAccessToken.isNotNullNorEmpty()) {
            onSuccess()
        }
        else {
            synergiaAccount()
        }
    }}

    private fun synergiaAccount(): Boolean {
        val accountLogin = data.apiLogin ?: return false
        val accessToken = data.portalAccessToken ?: return false
        data.callback.onActionStarted(R.string.sync_action_getting_account)
        d(TAG, "Requesting " + (LIBRUS_ACCOUNT_URL + accountLogin))
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
                .callback(object : JsonCallbackHandler() {
                    override fun onSuccess(json: JsonObject?, response: Response) {
                        if (json == null) {
                            data.callback.onError(null, AppError(TAG, 641, CODE_MAINTENANCE, response))
                            return
                        }
                        if (response.code() == 410) {
                            val reason = json.get("reason")
                            if (reason != null && reason !is JsonNull && reason.asString == "requires_an_action") {
                                data.callback.onError(null, AppError(TAG, 1078, CODE_LIBRUS_DISCONNECTED, response, json))
                                return
                            }
                            data.callback.onError(null, AppError(TAG, 70, CODE_INTERNAL_LIBRUS_ACCOUNT_410))
                            return
                        }
                        if (json.get("message") != null) {
                            val message = json.get("message").asString
                            if (message == "Account not found") {
                                data.callback.onError(null, AppError(TAG, 651, CODE_OTHER, data.app.getString(R.string.sync_error_register_student_not_associated_format, data.profile?.studentNameLong ?: "", accountLogin), response, json))
                                return
                            }
                            data.callback.onError(null, AppError(TAG, 654, CODE_OTHER, message + "\n\n" + accountLogin, response, json))
                            return
                        }
                        if (response.code() == HTTP_OK) {
                            try {
                                // synergiaAccount is executed when a synergia token needs a refresh
                                val accountId = json.getInt("id")
                                val accountToken = json.getString("accessToken")
                                if (accountId == null || accountToken == null) {
                                    data.callback.onError(null, AppError(TAG, 1284, CODE_OTHER, json))
                                    return
                                }
                                data.apiAccessToken = accountToken
                                data.apiTokenExpiryTime = currentTimeUnix() + 6*60*60
                                data.profile?.studentNameLong = json.getString("studentName")
                                val nameParts = json.getString("studentName")?.split(" ")?.toTypedArray()
                                data.profile?.studentNameShort = nameParts?.get(0) + " " + nameParts?.get(1)?.get(0)
                                onSuccess()
                            } catch (e: NullPointerException) {
                                e.printStackTrace()
                                data.callback.onError(null, AppError(TAG, 662, CODE_OTHER, response, e, json))
                            }

                        } else {
                            data.callback.onError(null, AppError(TAG, 425, CODE_OTHER, response, json))
                        }
                    }

                    override fun onFailure(response: Response, throwable: Throwable) {
                        data.callback.onError(null, AppError(TAG, 432, CODE_OTHER, response, throwable))
                    }
                })
                .build()
                .enqueue()
        return true
    }
}
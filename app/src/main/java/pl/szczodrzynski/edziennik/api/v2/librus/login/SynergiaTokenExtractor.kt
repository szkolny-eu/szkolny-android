package pl.szczodrzynski.edziennik.api.v2.librus.login

import com.google.gson.JsonNull
import com.google.gson.JsonObject
import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.JsonCallbackHandler
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.AppError
import pl.szczodrzynski.edziennik.api.AppError.*
import pl.szczodrzynski.edziennik.api.v2.LIBRUS_USER_AGENT
import pl.szczodrzynski.edziennik.api.interfaces.ProgressCallback
import pl.szczodrzynski.edziennik.api.v2.LIBRUS_ACCOUNT_URL
import pl.szczodrzynski.edziennik.datamodels.LoginStore
import pl.szczodrzynski.edziennik.datamodels.Profile
import pl.szczodrzynski.edziennik.utils.Utils.d
import java.net.HttpURLConnection.*

class SynergiaTokenExtractor(val app: App, val profile: Profile, val loginStore: LoginStore, val callback: ProgressCallback, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "librus.SynergiaToken"
    }

    init {
        val accountToken = profile.getStudentData("accountToken", null)
        val accountTokenTime = profile.getStudentData("accountTokenTime", 0L)
        if (accountToken.isNotNullNorEmpty() && currentTimeUnix() - accountTokenTime < 3 * 60 * 60) {
            onSuccess()
        }
        else {
            if (!synergiaAccount())
                callback.onError(null, AppError(TAG, 33, CODE_INTERNAL_MISSING_DATA))
        }
    }

    private fun synergiaAccount(): Boolean {
        val accountLogin = profile.getStudentData("accountLogin", null) ?: return false
        val tokenType = loginStore.getLoginData("tokenType", null) ?: return false
        val accessToken = loginStore.getLoginData("accessToken", null) ?: return false
        callback.onActionStarted(R.string.sync_action_getting_account)
        d(TAG, "Requesting " + (LIBRUS_ACCOUNT_URL + accountLogin))
        Request.builder()
                .url(LIBRUS_ACCOUNT_URL + accountLogin)
                .userAgent(LIBRUS_USER_AGENT)
                .addHeader("Authorization", "$tokenType $accessToken")
                .get()
                .allowErrorCode(HTTP_NOT_FOUND)
                .allowErrorCode(HTTP_FORBIDDEN)
                .allowErrorCode(HTTP_UNAUTHORIZED)
                .allowErrorCode(HTTP_BAD_REQUEST)
                .allowErrorCode(HTTP_GONE)
                .callback(object : JsonCallbackHandler() {
                    override fun onSuccess(data: JsonObject?, response: Response) {
                        if (data == null) {
                            callback.onError(null, AppError(TAG, 641, CODE_MAINTENANCE, response))
                            return
                        }
                        if (response.code() == 410) {
                            val reason = data.get("reason")
                            if (reason != null && reason !is JsonNull && reason.asString == "requires_an_action") {
                                callback.onError(null, AppError(TAG, 1078, CODE_LIBRUS_DISCONNECTED, response, data))
                                return
                            }
                            callback.onError(null, AppError(TAG, 70, CODE_INTERNAL_LIBRUS_ACCOUNT_410))
                            return
                        }
                        if (data.get("message") != null) {
                            val message = data.get("message").asString
                            if (message == "Account not found") {
                                callback.onError(null, AppError(TAG, 651, CODE_OTHER, app.getString(R.string.sync_error_register_student_not_associated_format, profile.studentNameLong, accountLogin), response, data))
                                return
                            }
                            callback.onError(null, AppError(TAG, 654, CODE_OTHER, message + "\n\n" + accountLogin, response, data))
                            return
                        }
                        if (response.code() == HTTP_OK) {
                            try {
                                // synergiaAccount is executed when a synergia token needs a refresh
                                val accountId = data.getInt("id")
                                val accountToken = data.getString("accessToken")
                                if (accountId == null || accountToken == null) {
                                    callback.onError(null, AppError(TAG, 1284, CODE_OTHER, data))
                                    return
                                }
                                profile.putStudentData("accountId", accountId)
                                profile.putStudentData("accountToken", accountToken)
                                profile.putStudentData("accountTokenTime", System.currentTimeMillis() / 1000)
                                profile.studentNameLong = data.getString("studentName")
                                val nameParts = data.getString("studentName")?.split(" ")?.toTypedArray()
                                profile.studentNameShort = nameParts?.get(0) + " " + nameParts?.get(1)?.get(0)
                                onSuccess()
                            } catch (e: NullPointerException) {
                                e.printStackTrace()
                                callback.onError(null, AppError(TAG, 662, CODE_OTHER, response, e, data))
                            }

                        } else {
                            callback.onError(null, AppError(TAG, 425, CODE_OTHER, response, data))
                        }
                    }

                    override fun onFailure(response: Response, throwable: Throwable) {
                        callback.onError(null, AppError(TAG, 432, CODE_OTHER, response, throwable))
                    }
                })
                .build()
                .enqueue()
        return true
    }
}
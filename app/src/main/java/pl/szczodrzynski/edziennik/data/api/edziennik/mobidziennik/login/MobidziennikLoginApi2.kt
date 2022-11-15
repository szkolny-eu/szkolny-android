/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-12.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.login

import android.os.Build
import com.google.gson.JsonObject
import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.JsonCallbackHandler
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.ext.JsonObject
import pl.szczodrzynski.edziennik.ext.getJsonObject
import pl.szczodrzynski.edziennik.ext.getString
import pl.szczodrzynski.edziennik.ext.isNotNullNorBlank
import pl.szczodrzynski.edziennik.ext.isNotNullNorEmpty
import pl.szczodrzynski.edziennik.utils.Utils

class MobidziennikLoginApi2(val data: DataMobidziennik, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "MobidziennikLoginApi2"

        fun getDevice(app: App) = JsonObject(
                "available" to true,
                "platform" to "Android",
                "version" to Build.VERSION.RELEASE,
                "uuid" to app.deviceId,
                "cordova" to "7.1.2",
                "model" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "manufacturer" to "Aplikacja Szkolny.eu",
                "isVirtual" to false,
                "serial" to try { System.getProperty("ro.serialno") ?: System.getProperty("ro.boot.serialno") } catch (_: Exception) { Build.UNKNOWN },
                "appVersion" to "10.6, 2020.01.09-12.15.53",
                "pushRegistrationId" to app.config.sync.tokenMobidziennik
        )
    }

    init { run {
        if (data.isApi2LoginValid()) {
            onSuccess()
        }
        else {
            if (data.loginServerName.isNotNullNorEmpty() && data.loginEmail.isNotNullNorEmpty() && data.loginPassword.isNotNullNorEmpty()) {
                loginWithCredentials()
            }
            else {
                data.error(ApiError(TAG, ERROR_LOGIN_DATA_MISSING))
            }
        }
    }}

    private fun loginWithCredentials() {
        Utils.d(TAG, "Request: Mobidziennik/Login/Api2 - https://mobidziennik.pl/logowanie")

        val callback = object : JsonCallbackHandler() {
            override fun onSuccess(json: JsonObject?, response: Response?) {
                if (json == null) {
                    data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }

                json.getJsonObject("error")?.let {
                    val text = it.getString("type") ?: it.getString("message")
                    when (text) {
                        "LOGIN_ERROR" -> ERROR_LOGIN_MOBIDZIENNIK_API2_INVALID_LOGIN
                        // TODO other error types
                        else -> ERROR_LOGIN_MOBIDZIENNIK_API2_OTHER
                    }.let { errorCode ->
                        data.error(ApiError(TAG, errorCode)
                                .withApiResponse(text)
                                .withResponse(response))
                        return
                    }
                }

                val email = json.getString("email")
                if (email.isNotNullNorBlank())
                    data.loginEmail = email
                data.globalId = json.getString("id_global")
                data.loginId = json.getString("login")
                onSuccess()
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                data.error(ApiError(TAG, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable))
            }
        }

        Request.builder()
                .url("https://mobidziennik.pl/logowanie")
                .userAgent(MOBIDZIENNIK_USER_AGENT)
                .contentType("application/x-www-form-urlencoded; charset=UTF-8")
                .addParameter("api2", true)
                .addParameter("email", data.loginEmail)
                .addParameter("haslo", data.loginPassword)
                .addParameter("device", getDevice(data.app).toString())
                .post()
                .callback(callback)
                .build()
                .enqueue()
    }
}

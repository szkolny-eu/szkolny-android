/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-12.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.api2

import com.google.gson.JsonObject
import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.JsonCallbackHandler
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.ENDPOINT_MOBIDZIENNIK_API2_MAIN
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.login.MobidziennikLoginApi2
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.getJsonObject
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.utils.Utils

class MobidziennikApi2Main(val data: DataMobidziennik,
                           val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "MobidziennikApi2Main"
    }

    val profileId
        get() = data.profile?.id ?: -1

    val profile
        get() = data.profile

    init {
        Utils.d(TAG, "Request: Mobidziennik/Api2/Main - https://${data.loginServerName}.mobidziennik.pl/api2/logowanie")

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

                val user = json.getJsonObject("user")
                data.ciasteczkoAutoryzacji = user.getString("auth_key")

                // sync always: this endpoint has .shouldSync set
                data.setSyncNext(ENDPOINT_MOBIDZIENNIK_API2_MAIN, SYNC_ALWAYS)
                data.app.config.sync.tokenMobidziennikList =
                        data.app.config.sync.tokenMobidziennikList + profileId
                onSuccess()
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                data.error(ApiError(TAG, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable))
            }
        }

        Request.builder()
                .url("https://${data.loginServerName}.mobidziennik.pl/api2/logowanie")
                .userAgent(MOBIDZIENNIK_USER_AGENT)
                .contentType("application/x-www-form-urlencoded; charset=UTF-8")
                .addParameter("login", data.loginId)
                .addParameter("email", data.loginEmail)
                .addParameter("haslo", data.loginPassword)
                .addParameter("device", MobidziennikLoginApi2.getDevice(data.app).toString())
                .apply {
                    data.ciasteczkoAutoryzacji?.let { addParameter("ciasteczko_autoryzacji", it) }
                }
                .post()
                .callback(callback)
                .build()
                .enqueue()
    }
}

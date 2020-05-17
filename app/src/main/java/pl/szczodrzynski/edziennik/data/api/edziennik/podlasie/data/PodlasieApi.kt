/*
 * Copyright (c) Kacper Ziubryniewicz 2020-5-12
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.data

import com.google.gson.JsonObject
import im.wangchao.mhttp.Request
import im.wangchao.mhttp.RequestParams
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.JsonCallbackHandler
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.DataPodlasie
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.getInt
import pl.szczodrzynski.edziennik.getJsonObject
import pl.szczodrzynski.edziennik.toHexString
import pl.szczodrzynski.edziennik.utils.Utils
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

open class PodlasieApi(open val data: DataPodlasie, open val lastSync: Long?) {
    companion object {
        const val TAG = "PodlasieApi"
    }

    val profileId
        get() = data.profile?.id ?: -1

    val profile
        get() = data.profile

    fun apiGet(tag: String, endpoint: String, onSuccess: (json: JsonObject) -> Unit) {
        val url = PODLASIE_API_URL + endpoint

        Utils.d(tag, "Request: Podlasie/Api - $url")

        if (data.apiToken == null) {
            data.error(tag, ERROR_PODLASIE_API_NO_TOKEN)
            return
        }

        val callback = object : JsonCallbackHandler() {
            override fun onSuccess(json: JsonObject?, response: Response?) {
                if (json == null || response == null) {
                    data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }

                val error = json.getJsonObject("system_message")?.getInt("code")

                error?.let { code ->
                    when (code) {
                        0 -> ERROR_PODLASIE_API_DATA_MISSING
                        4 -> ERROR_LOGIN_PODLASIE_API_DEVICE_LIMIT
                        5 -> ERROR_LOGIN_PODLASIE_API_INVALID_TOKEN
                        200 -> null // Not an error
                        else -> ERROR_PODLASIE_API_OTHER
                    }?.let { errorCode ->
                        data.error(ApiError(tag, errorCode)
                                .withApiResponse(json)
                                .withResponse(response))
                        return@onSuccess
                    }
                }

                try {
                    onSuccess(json)
                } catch (e: Exception) {
                    data.error(ApiError(tag, EXCEPTION_PODLASIE_API_REQUEST)
                            .withResponse(response)
                            .withThrowable(e)
                            .withApiResponse(json))
                }
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                data.error(ApiError(tag, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable))
            }
        }

        Request.builder()
                .url(url)
                .userAgent(SYSTEM_USER_AGENT)
                .requestParams(RequestParams(mapOf(
                        "token" to data.apiToken,
                        "securityToken" to getSecurityToken(),
                        "mobileId" to data.app.deviceId,
                        "ver" to PODLASIE_API_VERSION
                )))
                .callback(callback)
                .build()
                .enqueue()
    }

    private fun getSecurityToken(): String {
        val format = SimpleDateFormat("yyyy-MM-dd HH", Locale.ENGLISH)
                .also { it.timeZone = TimeZone.getTimeZone("Europe/Warsaw") }.format(System.currentTimeMillis())
        val instance = MessageDigest.getInstance("SHA-256")
        val digest = instance.digest("-EYlwYu8u16miVd8tT?oO7cvoUVQrQN0vr!$format".toByteArray()).toHexString()
        val digest2 = instance.digest(data.apiToken!!.toByteArray()).toHexString()
        return instance.digest("$digest$digest2".toByteArray()).toHexString()
    }
}

/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-19
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.JsonCallbackHandler
import io.github.wulkanowy.signer.android.signContent
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.modules.teams.Team
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.edziennik.utils.Utils.d
import java.net.HttpURLConnection
import java.util.*

open class VulcanApi(open val data: DataVulcan) {
    companion object {
        const val TAG = "VulcanApi"
    }

    val profileId
        get() = data.profile?.id ?: -1

    fun apiGet(
            tag: String,
            endpoint: String,
            method: Int = POST,
            parameters: Map<String, Any> = emptyMap(),
            baseUrl: Boolean = false,
            onSuccess: (json: JsonObject, response: Response?) -> Unit
    ) {
        val url = "${if (baseUrl) data.apiUrl else data.fullApiUrl}/$endpoint"

        d(tag, "Request: Vulcan/Api - $url")

        if (data.teamList.size() == 0) {
            data.profile?.studentClassName?.also { name ->
                val id = Utils.crc16(name.toByteArray()).toLong()

                val teamObject = Team(
                        profileId,
                        id,
                        name,
                        Team.TYPE_CLASS,
                        "${data.schoolName}:$name",
                        -1
                )
                data.teamList.put(id, teamObject)
            }
        }

        val finalPayload = JsonObject()
        parameters.map { (name, value) ->
            when (value) {
                is JsonObject -> finalPayload.add(name, value)
                is JsonArray -> finalPayload.add(name, value)
                is String -> finalPayload.addProperty(name, value)
                is Int -> finalPayload.addProperty(name, value)
                is Long -> finalPayload.addProperty(name, value)
                is Float -> finalPayload.addProperty(name, value)
                is Char -> finalPayload.addProperty(name, value)
            }
        }
        finalPayload.addProperty("RemoteMobileTimeKey", System.currentTimeMillis() / 1000)
        finalPayload.addProperty("TimeKey", System.currentTimeMillis() / 1000 - 1)
        finalPayload.addProperty("RequestId", UUID.randomUUID().toString())
        finalPayload.addProperty("RemoteMobileAppVersion", VULCAN_API_APP_VERSION)
        finalPayload.addProperty("RemoteMobileAppName", VULCAN_API_APP_NAME)

        val callback = object : JsonCallbackHandler() {
            override fun onSuccess(json: JsonObject?, response: Response?) {
                if (json == null && response?.parserErrorBody == null) {
                    data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }

                if (response?.code() ?: 200 != 200) {
                    when (response?.code()) {
                        503 -> ERROR_VULCAN_API_MAINTENANCE
                        400 -> ERROR_VULCAN_API_BAD_REQUEST
                        else -> ERROR_VULCAN_API_OTHER
                    }.let { errorCode ->
                        data.error(ApiError(tag, errorCode)
                                .withResponse(response)
                                .withApiResponse(json?.toString() ?: response?.parserErrorBody))
                        return
                    }
                }

                if (json == null) {
                    data.error(ApiError(tag, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }

                try {
                    onSuccess(json, response)
                } catch (e: Exception) {
                    data.error(ApiError(tag, EXCEPTION_VULCAN_API_REQUEST)
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
                .userAgent(VULCAN_API_USER_AGENT)
                .addHeader("RequestCertificateKey", data.apiCertificateKey)
                .addHeader("RequestSignatureValue",
                        try {
                            signContent(
                                    data.apiCertificatePrivate ?: "",
                                    finalPayload.toString()
                            )
                        } catch (e: Exception) {e.printStackTrace();""})
                .apply {
                    when (method) {
                        GET -> get()
                        POST -> post()
                    }
                }
                .setJsonBody(finalPayload)
                .allowErrorCode(HttpURLConnection.HTTP_BAD_REQUEST)
                .allowErrorCode(HttpURLConnection.HTTP_FORBIDDEN)
                .allowErrorCode(HttpURLConnection.HTTP_UNAUTHORIZED)
                .allowErrorCode(HttpURLConnection.HTTP_UNAVAILABLE)
                .callback(callback)
                .build()
                .enqueue()
    }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-29.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.TextCallbackHandler
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.utils.Utils
import java.net.HttpURLConnection

open class IdziennikApi(open val data: DataIdziennik) {
    companion object {
        const val TAG = "IdziennikApi"
    }

    val profileId
        get() = data.profile?.id ?: -1

    val profile
        get() = data.profile

    fun apiGet(tag: String, endpointTemplate: String, method: Int = GET, parameters: Map<String, Any> = emptyMap(), onSuccess: (json: JsonElement) -> Unit) {
        val endpoint = endpointTemplate.replace("\$STUDENT_ID", data.studentId ?: "")
        Utils.d(tag, "Request: Idziennik/API - $IDZIENNIK_API_URL/$endpoint")

        val callback = object : TextCallbackHandler() {
            override fun onSuccess(text: String?, response: Response?) {
                if (text == null) {
                    data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }

                val json = try {
                    JsonParser().parse(text)
                } catch (_: Exception) { null }

                var error: String? = null
                if (json == null) {
                    error = text
                }
                else if (json is JsonObject) {
                    error = if (response?.code() == 200) null else
                        json.getString("message") ?: json.toString()
                }
                error?.let { code ->
                    when (code) {
                        "Authorization has been denied for this request." -> ERROR_IDZIENNIK_API_ACCESS_DENIED
                        else -> ERROR_IDZIENNIK_API_OTHER
                    }.let { errorCode ->
                        data.error(ApiError(tag, errorCode)
                                .withApiResponse(text)
                                .withResponse(response))
                        return
                    }
                }

                try {
                    onSuccess(json!!)
                } catch (e: Exception) {
                    data.error(ApiError(tag, EXCEPTION_IDZIENNIK_API_REQUEST)
                            .withResponse(response)
                            .withThrowable(e)
                            .withApiResponse(text))
                }
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                data.error(ApiError(tag, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable))
            }
        }

        Request.builder()
                .url("$IDZIENNIK_API_URL/$endpoint")
                .userAgent(IDZIENNIK_API_USER_AGENT)
                .addHeader("Authorization", "Bearer ${data.apiBearer}")
                .apply {
                    when (method) {
                        GET -> get()
                        POST -> {
                            postJson()
                            val json = JsonObject()
                            parameters.map { (name, value) ->
                                when (value) {
                                    is JsonObject -> json.add(name, value)
                                    is JsonArray -> json.add(name, value)
                                    is String -> json.addProperty(name, value)
                                    is Int -> json.addProperty(name, value)
                                    is Long -> json.addProperty(name, value)
                                    is Float -> json.addProperty(name, value)
                                    is Char -> json.addProperty(name, value)
                                }
                            }
                            setJsonBody(json)
                        }
                    }
                }
                .allowErrorCode(HttpURLConnection.HTTP_UNAUTHORIZED)
                .allowErrorCode(HttpURLConnection.HTTP_INTERNAL_ERROR)
                .callback(callback)
                .build()
                .enqueue()
    }
}

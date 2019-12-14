/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-25.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.JsonCallbackHandler
import im.wangchao.mhttp.callback.TextCallbackHandler
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.utils.Utils.d
import java.net.HttpURLConnection.HTTP_INTERNAL_ERROR
import java.net.HttpURLConnection.HTTP_UNAUTHORIZED

open class IdziennikWeb(open val data: DataIdziennik) {
    companion object {
        const val TAG = "IdziennikWeb"
    }

    val profileId
        get() = data.profile?.id ?: -1

    val profile
        get() = data.profile

    fun webApiGet(tag: String, endpoint: String, parameters: Map<String, Any> = emptyMap(), onSuccess: (json: JsonObject) -> Unit) {
        d(tag, "Request: Idziennik/Web/API - $IDZIENNIK_WEB_URL/$endpoint")

        val callback = object : JsonCallbackHandler() {
            override fun onSuccess(json: JsonObject?, response: Response?) {
                if (json == null && response?.parserErrorBody == null) {
                    data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }

                when {
                    response?.code() == HTTP_UNAUTHORIZED -> ERROR_IDZIENNIK_WEB_ACCESS_DENIED
                    response?.code() == HTTP_INTERNAL_ERROR -> ERROR_IDZIENNIK_WEB_SERVER_ERROR
                    response?.parserErrorBody != null -> when {
                        response.parserErrorBody.contains("Identyfikator zgłoszenia") -> ERROR_IDZIENNIK_WEB_SERVER_ERROR
                        response.parserErrorBody.contains("Hasło dostępu do systemu wygasło") -> ERROR_IDZIENNIK_WEB_PASSWORD_CHANGE_NEEDED
                        response.parserErrorBody.contains("Trwają prace konserwacyjne") -> ERROR_IDZIENNIK_WEB_MAINTENANCE
                        else -> ERROR_IDZIENNIK_WEB_OTHER
                    }
                    else -> null
                }?.let { errorCode ->
                    data.error(ApiError(TAG, errorCode)
                            .withApiResponse(json?.toString() ?: response?.parserErrorBody)
                            .withResponse(response))
                    return
                }

                if (json == null) {
                    data.error(ApiError(tag, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }

                try {
                    onSuccess(json)
                } catch (e: Exception) {
                    data.error(ApiError(tag, EXCEPTION_IDZIENNIK_WEB_API_REQUEST)
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
                .url("$IDZIENNIK_WEB_URL/$endpoint")
                .userAgent(IDZIENNIK_USER_AGENT)
                .postJson()
                .apply {
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
                            is Boolean -> json.addProperty(name, value)
                        }
                    }
                    setJsonBody(json)
                }
                .allowErrorCode(HTTP_UNAUTHORIZED)
                .allowErrorCode(HTTP_INTERNAL_ERROR)
                .callback(callback)
                .build()
                .enqueue()
    }

    fun webGet(tag: String, endpoint: String, onSuccess: (text: String) -> Unit) {
        d(tag, "Request: Idziennik/Web - $IDZIENNIK_WEB_URL/$endpoint")

        val callback = object : TextCallbackHandler() {
            override fun onSuccess(text: String?, response: Response?) {
                if (text == null) {
                    data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }

                if (!text.contains("czyWyswietlicDostepMobilny")) {
                    when {
                        text.contains("Identyfikator zgłoszenia") -> ERROR_IDZIENNIK_WEB_SERVER_ERROR
                        text.contains("Hasło dostępu do systemu wygasło") -> ERROR_IDZIENNIK_WEB_PASSWORD_CHANGE_NEEDED
                        text.contains("Trwają prace konserwacyjne") -> ERROR_IDZIENNIK_WEB_MAINTENANCE
                        else -> ERROR_IDZIENNIK_WEB_OTHER
                    }.let { errorCode ->
                        data.error(ApiError(TAG, errorCode)
                                .withApiResponse(text)
                                .withResponse(response))
                        return
                    }
                }

                try {
                    onSuccess(text)
                } catch (e: Exception) {
                    data.error(ApiError(tag, EXCEPTION_IDZIENNIK_WEB_REQUEST)
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
                .url("$IDZIENNIK_WEB_URL/$endpoint")
                .userAgent(IDZIENNIK_USER_AGENT)
                .get()
                .callback(callback)
                .build()
                .enqueue()
    }
}

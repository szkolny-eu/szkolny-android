/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-21.
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data

import com.google.gson.JsonObject
import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.JsonCallbackHandler
import pl.szczodrzynski.edziennik.api.v2.*
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.utils.Utils.d
import java.net.HttpURLConnection.*

open class LibrusApi(open val data: DataLibrus) {
    companion object {
        private const val TAG = "LibrusApi"
    }

    val profileId
        get() = data.profile?.id ?: -1

    val profile
        get() = data.profile

    fun apiGet(tag: String, endpoint: String, method: Int = GET, payload: JsonObject? = null, onSuccess: (json: JsonObject) -> Unit) {

        d(tag, "Request: Librus/Api - $LIBRUS_API_URL/$endpoint")

        val callback = object : JsonCallbackHandler() {
            override fun onSuccess(json: JsonObject?, response: Response?) {
                if (json == null && response?.parserErrorBody == null) {
                    data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }
                val error = if (response?.code() == 200) null else
                    json.getString("Code") ?:
                    json.getString("Message") ?:
                    response?.parserErrorBody
                error?.let { code ->
                    when (code) {
                        "TokenIsExpired" -> ERROR_LIBRUS_API_TOKEN_EXPIRED
                        "Insufficient scopes" -> ERROR_LIBRUS_API_INSUFFICIENT_SCOPES
                        "Request is denied" -> ERROR_LIBRUS_API_ACCESS_DENIED
                        "Resource not found" -> ERROR_LIBRUS_API_RESOURCE_NOT_FOUND
                        "NotFound" -> ERROR_LIBRUS_API_DATA_NOT_FOUND
                        "AccessDeny" -> when (json.getString("Message")) {
                            "Student timetable is not public" -> ERROR_LIBRUS_API_TIMETABLE_NOT_PUBLIC
                            else -> ERROR_LIBRUS_API_RESOURCE_ACCESS_DENIED
                        }
                        "LuckyNumberIsNotActive" -> ERROR_LIBRUS_API_LUCKY_NUMBER_NOT_ACTIVE
                        "NotesIsNotActive" -> ERROR_LIBRUS_API_NOTES_NOT_ACTIVE
                        "InvalidRequest" -> ERROR_LIBRUS_API_INVALID_REQUEST_PARAMS
                        "Nieprawidłowy węzeł." -> ERROR_LIBRUS_API_INCORRECT_ENDPOINT
                        else -> ERROR_LIBRUS_API_OTHER
                    }.let { errorCode ->
                        data.error(ApiError(tag, errorCode)
                                .withApiResponse(json)
                                .withResponse(response))
                        return
                    }
                }

                if (json == null) {
                    data.error(ApiError(tag, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }

                try {
                    onSuccess(json)
                } catch (e: Exception) {
                    data.error(ApiError(tag, EXCEPTION_LIBRUS_API_REQUEST)
                            .withResponse(response)
                            .withThrowable(e)
                            .withApiResponse(json))
                }
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                // TODO add hotfix for Classrooms 500
                data.error(ApiError(tag, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable))
            }
        }

        Request.builder()
                .url("$LIBRUS_API_URL/$endpoint")
                .userAgent(LIBRUS_USER_AGENT)
                .addHeader("Authorization", "Bearer ${data.apiAccessToken}")
                .apply {
                    when (method) {
                        GET -> get()
                        POST -> post()
                    }
                    if (payload != null)
                        setJsonBody(payload)
                }
                .allowErrorCode(HTTP_BAD_REQUEST)
                .allowErrorCode(HTTP_FORBIDDEN)
                .allowErrorCode(HTTP_UNAUTHORIZED)
                .callback(callback)
                .build()
                .enqueue()
    }
}
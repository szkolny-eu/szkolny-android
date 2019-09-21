/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-21.
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data

import com.google.gson.JsonNull
import com.google.gson.JsonObject
import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.JsonCallbackHandler
import pl.szczodrzynski.edziennik.api.AppError
import pl.szczodrzynski.edziennik.api.AppError.CODE_MAINTENANCE
import pl.szczodrzynski.edziennik.api.AppError.CODE_OTHER
import pl.szczodrzynski.edziennik.api.v2.Api
import pl.szczodrzynski.edziennik.api.v2.CODE_INTERNAL_LIBRUS_SYNERGIA_EXPIRED
import pl.szczodrzynski.edziennik.api.v2.LIBRUS_API_URL
import pl.szczodrzynski.edziennik.api.v2.LIBRUS_USER_AGENT
import pl.szczodrzynski.edziennik.utils.Utils.d
import java.net.HttpURLConnection.*

open class LibrusApi(override val data: DataLibrus) : Api(data) {
    companion object {
        const val TAG = "LibrusApi"
    }
    fun apiRequest(endpoint: String, callback: (json: JsonObject?) -> Unit) {
        d(TAG, "Requesting $LIBRUS_API_URL$endpoint")
        Request.builder()
                .url(if (data.fakeLogin) "http://szkolny.eu/librus/api/$endpoint" else LIBRUS_API_URL + endpoint)
                .userAgent(LIBRUS_USER_AGENT)
                .addHeader("Authorization", "Bearer ${data.apiAccessToken}")
                .get()
                .allowErrorCode(HTTP_FORBIDDEN)
                .allowErrorCode(HTTP_UNAUTHORIZED)
                .allowErrorCode(HTTP_BAD_REQUEST)
                .callback(object : JsonCallbackHandler() {
                    override fun onSuccess(json: JsonObject?, response: Response) {
                        if (json == null) {
                            if (response.parserErrorBody != null && response.parserErrorBody == "Nieprawidłowy węzeł.") {
                                callback(null)
                                return
                            }
                            finishWithError(AppError(TAG, 453, CODE_MAINTENANCE, response))
                            return
                        }
                        if (json.get("Status") != null) {
                            val message = json.get("Message")
                            val code = json.get("Code")
                            d(TAG, "apiRequest Error " + json.get("Status").asString + " " + (if (message == null) "" else message.asString) + " " + (if (code == null) "" else code.asString) + "\n\n" + response.request().url().toString())
                            if (message != null && message !is JsonNull && message.asString == "Student timetable is not public") {
                                try {
                                    callback(null)
                                } catch (e: NullPointerException) {
                                    e.printStackTrace()
                                    d(TAG, "apiRequest exception " + e.message)
                                    finishWithError(AppError(TAG, 503, CODE_OTHER, response, e, json))
                                }

                                return
                            }
                            if (code != null
                                    && code !is JsonNull
                                    && (code.asString == "LuckyNumberIsNotActive"
                                            || code.asString == "NotesIsNotActive"
                                            || code.asString == "AccessDeny")) {
                                try {
                                    callback(null)
                                } catch (e: NullPointerException) {
                                    e.printStackTrace()
                                    d(TAG, "apiRequest exception " + e.message)
                                    finishWithError(AppError(TAG, 504, CODE_OTHER, response, e, json))
                                }

                                return
                            }
                            val errorText = json.get("Status").asString + " " + (if (message == null) "" else message.asString) + " " + if (code == null) "" else code.asString
                            if (code != null && code !is JsonNull && code.asString == "TokenIsExpired") {
                                finishWithError(AppError(TAG, 74, CODE_INTERNAL_LIBRUS_SYNERGIA_EXPIRED, errorText, response, json))
                                return
                            }
                            finishWithError(AppError(TAG, 497, CODE_OTHER, errorText, response, json))
                            return
                        }
                        try {
                            callback(json)
                        } catch (e: NullPointerException) {
                            e.printStackTrace()
                            d(TAG, "apiRequest exception " + e.message)
                            finishWithError(AppError(TAG, 505, CODE_OTHER, response, e, json))
                        }

                    }

                    override fun onFailure(response: Response, throwable: Throwable) {
                        if (response.code() == 405) {
                            // method not allowed
                            finishWithError(AppError(TAG, 511, CODE_OTHER, response, throwable))
                            return
                        }
                        if (response.code() == 500) {
                            // TODO: 2019-09-10 dirty hotfix
                            if ("Classrooms" == endpoint) {
                                callback(null)
                                return
                            }
                            finishWithError(AppError(TAG, 516, CODE_MAINTENANCE, response, throwable))
                            return
                        }
                        finishWithError(AppError(TAG, 520, CODE_OTHER, response, throwable))
                    }
                })
                .build()
                .enqueue()
    }
}
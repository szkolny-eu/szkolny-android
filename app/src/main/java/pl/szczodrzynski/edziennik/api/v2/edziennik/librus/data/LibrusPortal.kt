package pl.szczodrzynski.edziennik.api.v2.edziennik.librus.data

import com.google.gson.JsonObject
import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.JsonCallbackHandler
import pl.szczodrzynski.edziennik.api.v2.*
import pl.szczodrzynski.edziennik.api.v2.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.utils.Utils.d
import java.net.HttpURLConnection

open class LibrusPortal(open val data: DataLibrus) {
    companion object {
        private const val TAG = "LibrusPortal"
    }

    val profileId
        get() = data.profile?.id ?: -1

    val profile
        get() = data.profile

    fun portalGet(tag: String, endpoint: String, method: Int = GET, payload: JsonObject? = null, onSuccess: (json: JsonObject, response: Response?) -> Unit) {

        d(tag, "Request: Librus/Portal - ${if (data.fakeLogin) FAKE_LIBRUS_PORTAL else LIBRUS_PORTAL_URL}$endpoint")

        val callback = object : JsonCallbackHandler() {
            override fun onSuccess(json: JsonObject?, response: Response?) {
                if (json == null) {
                    data.error(ApiError(tag, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }
                val error = if (response?.code() == 200) null else
                    json.getString("reason") ?:
                    json.getString("message") ?:
                    json.getString("hint") ?:
                    json.getString("Code")
                error?.let { code ->
                    when (code) {
                        "requires_an_action" -> ERROR_LIBRUS_PORTAL_SYNERGIA_DISCONNECTED
                        "Access token is invalid" -> ERROR_LIBRUS_PORTAL_ACCESS_DENIED
                        "ApiDisabled" -> ERROR_LIBRUS_PORTAL_API_DISABLED
                        "Account not found" -> ERROR_LIBRUS_PORTAL_SYNERGIA_NOT_FOUND
                        else -> when (json.getString("hint")) {
                            "Error while decoding to JSON" -> ERROR_LIBRUS_PORTAL_ACCESS_DENIED
                            else -> ERROR_LIBRUS_PORTAL_OTHER
                        }
                    }.let { errorCode ->
                        data.error(ApiError(tag, errorCode)
                                .withApiResponse(json)
                                .withResponse(response))
                        return
                    }
                }
                if (response?.code() == HttpURLConnection.HTTP_OK) {
                    try {
                        onSuccess(json, response)
                    } catch (e: NullPointerException) {
                        e.printStackTrace()
                        data.error(ApiError(tag, EXCEPTION_LIBRUS_PORTAL_SYNERGIA_TOKEN)
                                .withResponse(response)
                                .withThrowable(e)
                                .withApiResponse(json))
                    }

                } else {
                    data.error(ApiError(tag, ERROR_REQUEST_FAILURE)
                            .withResponse(response)
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
                .url((if (data.fakeLogin) FAKE_LIBRUS_PORTAL else LIBRUS_PORTAL_URL) + endpoint)
                .userAgent(LIBRUS_USER_AGENT)
                .addHeader("Authorization", "Bearer ${data.portalAccessToken}")
                .apply {
                    when (method) {
                        GET -> get()
                        POST -> post()
                    }
                    if (payload != null)
                        setJsonBody(payload)
                }
                .allowErrorCode(HttpURLConnection.HTTP_NOT_FOUND)
                .allowErrorCode(HttpURLConnection.HTTP_FORBIDDEN)
                .allowErrorCode(HttpURLConnection.HTTP_UNAUTHORIZED)
                .allowErrorCode(HttpURLConnection.HTTP_BAD_REQUEST)
                .allowErrorCode(HttpURLConnection.HTTP_GONE)
                .callback(callback)
                .build()
                .enqueue()
    }
}

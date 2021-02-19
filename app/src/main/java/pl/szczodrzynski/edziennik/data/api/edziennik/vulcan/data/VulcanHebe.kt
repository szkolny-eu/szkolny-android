package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data

import android.os.Build
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.body.MediaTypeUtils
import im.wangchao.mhttp.callback.JsonCallbackHandler
import io.github.wulkanowy.signer.hebe.getSignatureHeaders
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.utils.Utils.d
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

open class VulcanHebe(open val data: DataVulcan, open val lastSync: Long?) {
    companion object {
        const val TAG = "VulcanHebe"
    }

    val profileId
        get() = data.profile?.id ?: -1

    val profile
        get() = data.profile

    inline fun <reified T> apiRequest(
        tag: String,
        endpoint: String,
        method: Int = GET,
        payload: JsonObject? = null,
        baseUrl: Boolean = false,
        crossinline onSuccess: (json: T, response: Response?) -> Unit
    ) {
        val url = "${if (baseUrl) data.apiUrl else data.fullApiUrl}$endpoint"

        d(tag, "Request: Vulcan/Hebe - $url")

        val privateKey = data.hebePrivateKey
        val publicHash = data.hebePublicHash

        if (privateKey == null || publicHash == null) {
            data.error(ApiError(TAG, ERROR_LOGIN_DATA_MISSING))
            return
        }

        val timestamp = ZonedDateTime.now(ZoneId.of("GMT"))
        val timestampMillis = timestamp.toInstant().toEpochMilli()
        val timestampIso = timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss"))

        val finalPayload = if (payload != null) {
            JsonObject(
                "AppName" to VULCAN_HEBE_APP_NAME,
                "AppVersion" to VULCAN_HEBE_APP_VERSION,
                "CertificateId" to publicHash,
                "Envelope" to payload,
                "FirebaseToken" to data.app.config.sync.tokenVulcanHebe,
                "API" to 1,
                "RequestId" to UUID.randomUUID().toString(),
                "Timestamp" to timestampMillis,
                "TimestampFormatted" to timestampIso
            )
        } else null
        val jsonString = finalPayload?.toString()

        val headers = getSignatureHeaders(
            publicHash,
            privateKey,
            jsonString,
            endpoint,
            timestamp
        )

        val callback = object : JsonCallbackHandler() {
            override fun onSuccess(json: JsonObject?, response: Response?) {
                if (json == null) {
                    data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                            .withResponse(response)
                    )
                    return
                }

                val status = json.getJsonObject("Status")
                if (status?.getInt("Code") != 0) {
                    data.error(ApiError(tag, ERROR_VULCAN_HEBE_OTHER)
                        .withResponse(response)
                        .withApiResponse(json.toString()))
                }

                val envelope = when (T::class.java) {
                    JsonObject::class.java -> json.getJsonObject("Envelope")
                    JsonArray::class.java -> json.getJsonArray("Envelope")
                    else -> {
                        data.error(ApiError(tag, ERROR_RESPONSE_EMPTY)
                            .withResponse(response)
                            .withApiResponse(json)
                        )
                        return
                    }
                }

                try {
                    onSuccess(envelope as T, response)
                } catch (e: Exception) {
                    data.error(ApiError(tag, EXCEPTION_VULCAN_HEBE_REQUEST)
                            .withResponse(response)
                            .withThrowable(e)
                            .withApiResponse(json)
                    )
                }
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                data.error(ApiError(tag, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable)
                )
            }
        }

        Request.builder()
            .url(url)
            .userAgent(VULCAN_HEBE_USER_AGENT)
            .addHeader("vOS", "Android")
            .addHeader("vDeviceModel", Build.MODEL)
            .addHeader("vAPI", "1")
            .apply {
                headers.forEach {
                    addHeader(it.key, it.value)
                }
                when (method) {
                    GET -> get()
                    POST -> {
                        post()
                        setTextBody(jsonString, MediaTypeUtils.APPLICATION_JSON)
                    }
                }
            }
            .allowErrorCode(HttpURLConnection.HTTP_BAD_REQUEST)
            .allowErrorCode(HttpURLConnection.HTTP_FORBIDDEN)
            .allowErrorCode(HttpURLConnection.HTTP_UNAUTHORIZED)
            .allowErrorCode(HttpURLConnection.HTTP_UNAVAILABLE)
            .callback(callback)
            .build()
            .enqueue()
    }

    inline fun <reified T> apiGet(
        tag: String,
        endpoint: String,
        query: Map<String, String> = mapOf(),
        baseUrl: Boolean = false,
        crossinline onSuccess: (json: T, response: Response?) -> Unit
    ) {
        val queryPath = query.map { it.key + "=" + URLEncoder.encode(it.value, "UTF-8") }.join("&")
        apiRequest(
            tag,
            if (query.isNotEmpty()) "$endpoint?$queryPath" else endpoint,
            baseUrl = baseUrl,
            onSuccess = onSuccess
        )
    }

    inline fun <reified T> apiPost(
        tag: String,
        endpoint: String,
        payload: JsonObject,
        baseUrl: Boolean = false,
        crossinline onSuccess: (json: T, response: Response?) -> Unit
    ) {
        apiRequest(
            tag,
            endpoint,
            method = POST,
            payload,
            baseUrl = baseUrl,
            onSuccess = onSuccess
        )
    }
}

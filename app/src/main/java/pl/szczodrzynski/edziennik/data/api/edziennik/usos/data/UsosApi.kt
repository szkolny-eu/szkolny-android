/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-13.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.usos.data

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.body.MediaTypeUtils
import im.wangchao.mhttp.callback.JsonArrayCallbackHandler
import im.wangchao.mhttp.callback.JsonCallbackHandler
import im.wangchao.mhttp.callback.TextCallbackHandler
import pl.szczodrzynski.edziennik.data.api.ERROR_REQUEST_FAILURE
import pl.szczodrzynski.edziennik.data.api.ERROR_USOS_API_MISSING_RESPONSE
import pl.szczodrzynski.edziennik.data.api.SERVER_USER_AGENT
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.DataUsos
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.login.UsosLoginApi
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.ext.*
import timber.log.Timber
import java.net.HttpURLConnection.*
import java.util.UUID

open class UsosApi(open val data: DataUsos, open val lastSync: Long?) {
    companion object {
        private const val TAG = "UsosApi"
    }

    enum class ResponseType {
        OBJECT,
        ARRAY,
        PLAIN,
    }

    val profileId
        get() = data.profile?.id ?: -1

    val profile
        get() = data.profile

    protected fun JsonObject.getLangString(key: String) =
        this.getJsonObject(key)?.getString("pl")

    protected fun JsonObject.getLecturerIds(key: String) =
        this.getJsonArray(key)?.asJsonObjectList()?.mapNotNull {
            val id = it.getLong("id") ?: return@mapNotNull null
            val firstName = it.getString("first_name") ?: return@mapNotNull null
            val lastName = it.getString("last_name") ?: return@mapNotNull null
            data.getTeacher(firstName, lastName, id = id).id
        } ?: listOf()

    private fun valueToString(value: Any) = when (value) {
        is String -> value
        is Number -> value.toString()
        is List<*> -> listToString(value)
        else -> value.toString()
    }

    private fun listToString(list: List<*>): String {
        return list.map {
            if (it is Pair<*, *> && it.first is String && it.second is List<*>)
                return@map "${it.first}[${listToString(it.second as List<*>)}]"
            return@map valueToString(it ?: "")
        }.joinToString("|")
    }

    private fun buildSignature(method: String, url: String, params: Map<String, String>): String {
        val query = params.toQueryString()
        val signatureString = listOf(
            method.uppercase(),
            url.urlEncode(),
            query.urlEncode(),
        ).joinToString("&")
        val signingKey = listOf(
            data.oauthConsumerSecret ?: "",
            data.oauthTokenSecret ?: "",
        ).joinToString("&") { it.urlEncode() }
        return signatureString.hmacSHA1(signingKey)
    }

    fun <T> apiRequest(
        tag: String,
        service: String,
        params: Map<String, Any>? = null,
        fields: List<Any>? = null,
        responseType: ResponseType,
        onSuccess: (data: T, response: Response?) -> Unit,
    ) {
        val url = "${data.instanceUrl}services/$service"
        Timber.tag(tag).d("Request: Usos/Api - $url")

        val formData = mutableMapOf<String, String>()
        if (params != null)
            formData.putAll(params.mapValues {
                valueToString(it.value)
            })
        if (fields != null)
            formData["fields"] = valueToString(fields)

        val auth = mutableMapOf(
            "realm" to url,
            "oauth_consumer_key" to (data.oauthConsumerKey ?: ""),
            "oauth_nonce" to UUID.randomUUID().toString(),
            "oauth_signature_method" to "HMAC-SHA1",
            "oauth_timestamp" to currentTimeUnix().toString(),
            "oauth_token" to (data.oauthTokenKey ?: ""),
            "oauth_version" to "1.0",
        )
        val signature = buildSignature(
            method = "POST",
            url = url,
            params = formData + auth.filterKeys { it.startsWith("oauth_") },
        )
        auth["oauth_signature"] = signature

        val authString = auth.map {
            """${it.key}="${it.value.urlEncode()}""""
        }.joinToString(", ")

        Request.builder()
            .url(url)
            .userAgent(SERVER_USER_AGENT)
            .addHeader("Authorization", "OAuth $authString")
            .post()
            .setTextBody(formData.toQueryString(), MediaTypeUtils.APPLICATION_FORM)
            .allowErrorCode(HTTP_BAD_REQUEST)
            .allowErrorCode(HTTP_UNAUTHORIZED)
            .allowErrorCode(HTTP_FORBIDDEN)
            .allowErrorCode(HTTP_NOT_FOUND)
            .allowErrorCode(HTTP_UNAVAILABLE)
            .callback(getCallback(tag, responseType, onSuccess))
            .build()
            .enqueue()
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getCallback(
        tag: String,
        responseType: ResponseType,
        onSuccess: (data: T, response: Response?) -> Unit,
    ) = when (responseType) {
        ResponseType.OBJECT -> object : JsonCallbackHandler() {
            override fun onSuccess(data: JsonObject?, response: Response) {
                processResponse(tag, response, data as T?, onSuccess)
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                processError(tag, response, throwable)
            }
        }
        ResponseType.ARRAY -> object : JsonArrayCallbackHandler() {
            override fun onSuccess(data: JsonArray?, response: Response) {
                processResponse(tag, response, data as T?, onSuccess)
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                processError(tag, response, throwable)
            }
        }
        ResponseType.PLAIN -> object : TextCallbackHandler() {
            override fun onSuccess(data: String?, response: Response) {
                processResponse(tag, response, data as T?, onSuccess)
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                processError(tag, response, throwable)
            }
        }
    }

    private fun <T> processResponse(
        tag: String,
        response: Response,
        value: T?,
        onSuccess: (data: T, response: Response?) -> Unit,
    ) {
        val errorCode = when {
            response.code() == HTTP_UNAUTHORIZED -> {
                data.oauthTokenKey = null
                data.oauthTokenSecret = null
                data.oauthTokenIsUser = false
                data.oauthLoginResponse = null
                UsosLoginApi(data) { }
                return
            }
            value == null -> ERROR_USOS_API_MISSING_RESPONSE
            response.code() == HTTP_OK -> {
                onSuccess(value, response)
                null
            }
            else -> response.toErrorCode()
        }
        if (errorCode != null) {
            data.error(tag, errorCode, response, value.toString())
        }
    }

    private fun processError(
        tag: String,
        response: Response?,
        throwable: Throwable?,
    ) {
        data.error(ApiError(tag, ERROR_REQUEST_FAILURE)
            .withResponse(response)
            .withThrowable(throwable))
    }
}

/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-21.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data

import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.TextCallbackHandler
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import timber.log.Timber

open class LibrusSynergia(open val data: DataLibrus, open val lastSync: Long?) {
    companion object {
        private const val TAG = "LibrusSynergia"
    }

    val profileId
        get() = data.profile?.id ?: -1

    val profile
        get() = data.profile

    fun synergiaGet(tag: String, endpoint: String, method: Int = GET,
                    parameters: Map<String, Any> = emptyMap(), onSuccess: (text: String) -> Unit) {
        Timber.tag(tag).d("Request: Librus/Synergia - $LIBRUS_SYNERGIA_URL/$endpoint")

        val callback = object : TextCallbackHandler() {
            override fun onSuccess(text: String?, response: Response?) {
                if (text.isNullOrEmpty()) {
                    data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }

                if (!text.contains("jesteś zalogowany") && !text.contains("Podgląd zadania")) {
                    when {
                        text.contains("stop.png") -> ERROR_LIBRUS_SYNERGIA_ACCESS_DENIED
                        text.contains("Przerwa techniczna") -> ERROR_LIBRUS_SYNERGIA_MAINTENANCE
                        else -> ERROR_LIBRUS_SYNERGIA_OTHER
                    }.let { errorCode ->
                        data.error(ApiError(tag, errorCode)
                                .withResponse(response)
                                .withApiResponse(text))
                        return
                    }
                }

                try {
                    onSuccess(text)
                } catch (e: Exception) {
                    data.error(ApiError(tag, EXCEPTION_LIBRUS_SYNERGIA_REQUEST)
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

        /*data.app.cookieJar.saveFromResponse(null, listOf(
                Cookie.Builder()
                        .name("DZIENNIKSID")
                        .value(data.synergiaSessionId!!)
                        .domain("synergia.librus.pl")
                        .secure().httpOnly().build()
        ))*/

        Request.builder()
                .url("$LIBRUS_SYNERGIA_URL/$endpoint")
                .userAgent(LIBRUS_USER_AGENT)
                .apply {
                    when (method) {
                        GET -> get()
                        POST -> post()
                    }
                    parameters.map { (name, value) ->
                        addParameter(name, value)
                    }
                }
                .callback(callback)
                .build()
                .enqueue()
    }

    fun redirectUrlGet(tag: String, url: String, onSuccess: (url: String) -> Unit) {
        Timber.tag(tag).d("Request: Librus/Synergia - $url")

        val callback = object : TextCallbackHandler() {
            override fun onSuccess(text: String?, response: Response) {
                val redirectUrl = response.headers().get("Location")

                if (redirectUrl != null) {
                    try {
                        onSuccess(redirectUrl)
                    } catch (e: Exception) {
                        data.error(ApiError(tag, EXCEPTION_LIBRUS_SYNERGIA_REQUEST)
                                .withResponse(response)
                                .withThrowable(e)
                                .withApiResponse(text))
                    }
                } else {
                    data.error(ApiError(tag, ERROR_LIBRUS_SYNERGIA_OTHER)
                            .withResponse(response)
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
                .url(url)
                .userAgent(LIBRUS_USER_AGENT)
                .withClient(data.app.httpLazy)
                .get()
                .callback(callback)
                .build()
                .enqueue()
    }
}

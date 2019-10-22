/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-21.
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data

import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.TextCallbackHandler
import okhttp3.Cookie
import pl.szczodrzynski.edziennik.api.v2.*
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.utils.Utils.d
import java.lang.Exception

open class LibrusSynergia(open val data: DataLibrus) {
    companion object {
        const val TAG = "LibrusSynergia"
    }

    val profileId
        get() = data.profile?.id ?: -1

    val profile
        get() = data.profile

    fun apiGet(tag: String, endpoint: String, method: Int = GET,
               parameters: Map<String, Any> = emptyMap(), onSuccess: (text: String) -> Unit) {
        d(tag, "Request: Librus/Synergia - $LIBRUS_SYNERGIA_URL$endpoint")

        val callback = object : TextCallbackHandler() {
            override fun onSuccess(text: String?, response: Response?) {
                if (text.isNullOrEmpty()) {
                    data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }

                // TODO: Error handling

                try {
                    onSuccess(text)
                } catch (e: Exception) {
                    data.error(ApiError(tag, EXCEPTION_LIBRUS_SYNERGIA_REQUEST)
                            .withResponse(response)
                            .withThrowable(e)
                            .withApiResponse(text))
                }
            }
        }

        data.app.cookieJar.saveFromResponse(null, listOf(
                Cookie.Builder()
                        .name("DZIENNIKSID")
                        .value(data.synergiaSessionId!!)
                        .domain("synergia.librus.pl")
                        .secure().httpOnly().build()
        ))

        Request.builder()
                .url("$LIBRUS_SYNERGIA_URL$endpoint")
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
}

/*
 * Copyright (c) Kacper Ziubryniewicz 2020-5-17
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.gdynia.data

import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.TextCallbackHandler
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.gdynia.DataGdynia
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.utils.Utils.d

open class GdyniaWeb(open val data: DataGdynia, open val lastSync: Long?) {
    companion object {
        private const val TAG = "GdyniaWeb"
    }

    fun webGet(tag: String, endpoint: String, parameters: Map<String, Any?>? = null, onSuccess: (text: String) -> Unit) {
        val url = "$GDYNIA_WEB_URL/$endpoint"
        d(tag, "Request: Gdynia/Web - $url")

        val callback = object : TextCallbackHandler() {
            override fun onSuccess(text: String?, response: Response?) {
                if (text == null || response == null) {
                    data.error(ApiError(tag, ERROR_RESPONSE_EMPTY)
                            .withResponse(response)
                            .withApiResponse(text))
                    return
                }

                try {
                    onSuccess(text)
                } catch (e: Exception) {
                    data.error(ApiError(tag, EXCEPTION_GDYNIA_WEB_REQUEST)
                            .withApiResponse(text)
                            .withResponse(response)
                            .withThrowable(e))
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
                .userAgent(SYSTEM_USER_AGENT)
                .apply {
                    parameters?.forEach { (key, value) ->
                        addParameter(key, value)
                    }
                }
                .get()
                .callback(callback)
                .build()
                .enqueue()
    }
}

/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-22
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data

import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.TextCallbackHandler
import okhttp3.Cookie
import pl.szczodrzynski.edziennik.data.api.EDUDZIENNIK_USER_AGENT
import pl.szczodrzynski.edziennik.data.api.ERROR_REQUEST_FAILURE
import pl.szczodrzynski.edziennik.data.api.ERROR_RESPONSE_EMPTY
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.utils.Utils.d

open class EdudziennikWeb(open val data: DataEdudziennik) {
    companion object {
        private const val TAG = "EdudziennikWeb"
    }

    val profileId
        get() = data.profile?.id ?: -1

    val profile
        get() = data.profile

    fun webGet(tag: String, endpoint: String, xhr: Boolean = false, onSuccess: (text: String) -> Unit) {
        val url = "https://dziennikel.appspot.com/" + when (endpoint.endsWith('/') || endpoint.contains('?') || endpoint.isEmpty()) {
            true -> endpoint
            else -> "$endpoint/"
        }

        d(tag, "Request: Edudziennik/Web - $url")

        val callback = object : TextCallbackHandler() {
            override fun onSuccess(text: String?, response: Response?) {
                if (text == null || response == null) {
                    data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }

                onSuccess(text)
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                data.error(ApiError(TAG, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable))
            }
        }

        data.app.cookieJar.saveFromResponse(null, listOf(
                Cookie.Builder()
                        .name("sessionid")
                        .value(data.webSessionId!!)
                        .domain("dziennikel.appspot.com")
                        .secure().httpOnly().build(),
                Cookie.Builder()
                        .name("semester")
                        .value((profile?.currentSemester ?: 1).toString())
                        .domain("dziennikel.appspot.com")
                        .secure().httpOnly().build()
        ))

        Request.builder()
                .url(url)
                .userAgent(EDUDZIENNIK_USER_AGENT)
                .apply {
                    if (xhr) header("X-Requested-With", "XMLHttpRequest")
                }
                .get()
                .callback(callback)
                .build()
                .enqueue()
    }
}

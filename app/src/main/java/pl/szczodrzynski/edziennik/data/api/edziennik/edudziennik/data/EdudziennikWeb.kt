/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-22
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data

import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.TextCallbackHandler
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.utils.Utils.d
import pl.szczodrzynski.edziennik.utils.models.Date

open class EdudziennikWeb(open val data: DataEdudziennik, open val lastSync: Long?) {
    companion object {
        private const val TAG = "EdudziennikWeb"
    }

    val profileId
        get() = data.profile?.id ?: -1

    val profile
        get() = data.profile

    fun webGet(tag: String, endpoint: String, xhr: Boolean = false, semester: Int? = null, onSuccess: (text: String) -> Unit) {
        val url = "https://dziennikel.appspot.com/" + when (endpoint.endsWith('/') || endpoint.contains('?') || endpoint.isEmpty()) {
            true -> endpoint
            else -> "$endpoint/"
        } + (semester?.let { "?semester=" + if(it == -1) "all" else it } ?: "")

        d(tag, "Request: Edudziennik/Web - $url")

        val callback = object : TextCallbackHandler() {
            override fun onSuccess(text: String?, response: Response?) {
                if (text == null || response == null) {
                    data.error(ApiError(tag, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }

                if (semester == null && url.contains("start")) {
                    profile?.also { profile ->
                        val cookies = data.app.cookieJar.getAll("dziennikel.appspot.com")
                        val semesterCookie = cookies["semester"]?.toIntOrNull()

                        semesterCookie?.let { data.currentSemester = it }

                        if (semesterCookie == 2 && profile.dateSemester2Start > Date.getToday())
                            profile.dateSemester2Start = Date.getToday().stepForward(0, 0, -1)
                    }
                }

                try {
                    onSuccess(text)
                } catch (e: Exception) {
                    data.error(ApiError(tag, EXCEPTION_EDUDZIENNIK_WEB_REQUEST)
                            .withThrowable(e)
                            .withResponse(response)
                            .withApiResponse(text))
                }
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                val error = when (response?.code()) {
                    402 -> ERROR_EDUDZIENNIK_WEB_LIMITED_ACCESS
                    403 -> ERROR_EDUDZIENNIK_WEB_SESSION_EXPIRED
                    else -> ERROR_REQUEST_FAILURE
                }
                data.error(ApiError(tag, error)
                        .withResponse(response)
                        .withThrowable(throwable))
            }
        }

        data.app.cookieJar.set("dziennikel.appspot.com", "sessionid", data.webSessionId)

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

/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.api.v2.mobidziennik.data

import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.TextCallbackHandler
import okhttp3.Cookie
import pl.szczodrzynski.edziennik.api.v2.*
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.utils.Utils.d

open class MobidziennikWeb(open val data: DataMobidziennik) {
    companion object {
        private const val TAG = "MobidziennikWeb"
    }

    val profileId
        get() = data.profile?.id ?: -1

    val profile
        get() = data.profile

    fun webGet(tag: String, endpoint: String, method: Int = GET, payload: List<Pair<String, String>>? = null, onSuccess: (text: String) -> Unit) {
        val url = "https://${data.loginServerName}.mobidziennik.pl$endpoint"

        d(tag, "Request: Mobidziennik/Web - $url")

        if (data.webSessionKey == null) {
            data.error(TAG, ERROR_MOBIDZIENNIK_WEB_NO_SESSION_KEY)
            return
        }
        if (data.webSessionValue == null) {
            data.error(TAG, ERROR_MOBIDZIENNIK_WEB_NO_SESSION_VALUE)
            return
        }
        if (data.webServerId == null) {
            data.error(TAG, ERROR_MOBIDZIENNIK_WEB_NO_SERVER_ID)
            return
        }

        val callback = object : TextCallbackHandler() {
            override fun onSuccess(text: String?, response: Response?) {
                if (text.isNullOrEmpty()) {
                    data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }
                if (text == "Nie jestes zalogowany"
                        || text.contains("przypomnij_haslo_email")) {
                    data.error(ApiError(TAG, ERROR_MOBIDZIENNIK_WEB_ACCESS_DENIED)
                            .withResponse(response))
                    return
                }

                try {
                    onSuccess(text)
                } catch (e: Exception) {
                    data.error(ApiError(tag, EXCEPTION_MOBIDZIENNIK_WEB_REQUEST)
                            .withResponse(response)
                            .withThrowable(e)
                            .withApiResponse(text))
                }
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                data.error(ApiError(TAG, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable))
            }
        }

        data.app.cookieJar.saveFromResponse(null, listOf(
                Cookie.Builder()
                        .name(data.webSessionKey!!)
                        .value(data.webSessionValue!!)
                        .domain("${data.loginServerName}.mobidziennik.pl")
                        .secure().httpOnly().build(),
                Cookie.Builder()
                        .name("SERVERID")
                        .value(data.webServerId!!)
                        .domain("${data.loginServerName}.mobidziennik.pl")
                        .secure().httpOnly().build()
        ))

        Request.builder()
                .url(url)
                .userAgent(MOBIDZIENNIK_USER_AGENT)
                .callback(callback)
                .build()
                .enqueue()
    }
}
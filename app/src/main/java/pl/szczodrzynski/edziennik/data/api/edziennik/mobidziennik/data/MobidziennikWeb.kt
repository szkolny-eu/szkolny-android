/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data

import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.FileCallbackHandler
import im.wangchao.mhttp.callback.TextCallbackHandler
import okhttp3.Cookie
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.utils.Utils.d
import java.io.File

open class MobidziennikWeb(open val data: DataMobidziennik) {
    companion object {
        private const val TAG = "MobidziennikWeb"
    }

    val profileId
        get() = data.profile?.id ?: -1

    val profile
        get() = data.profile

    fun webGet(tag: String, endpoint: String, method: Int = GET, onSuccess: (text: String) -> Unit) {
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

    fun webGetFile(tag: String, action: String, targetFile: File, onSuccess: (file: File) -> Unit,
                   onProgress: (written: Long, total: Long) -> Unit) {
        val url = "https://${data.loginServerName}.mobidziennik.pl$action"

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

        val callback = object : FileCallbackHandler(targetFile) {
            override fun onSuccess(file: File?, response: Response?) {
                if (file == null) {
                    data.error(ApiError(TAG, ERROR_FILE_DOWNLOAD)
                            .withResponse(response))
                    return
                }

                try {
                    onSuccess(file)
                } catch (e: Exception) {
                    data.error(ApiError(tag, EXCEPTION_MOBIDZIENNIK_WEB_FILE_REQUEST)
                            .withResponse(response)
                            .withThrowable(e))
                }
            }

            override fun onProgress(bytesWritten: Long, bytesTotal: Long) {
                try {
                    onProgress(bytesWritten, bytesTotal)
                } catch (e: Exception) {
                    data.error(ApiError(tag, EXCEPTION_MOBIDZIENNIK_WEB_FILE_REQUEST)
                            .withThrowable(e))
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

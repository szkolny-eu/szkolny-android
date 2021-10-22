/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-5.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.login

import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.TextCallbackHandler
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.Mobidziennik
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.ext.getUnixDate
import pl.szczodrzynski.edziennik.ext.isNotNullNorEmpty
import pl.szczodrzynski.edziennik.utils.Utils.d

class MobidziennikLoginWeb(val data: DataMobidziennik, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "MobidziennikLoginWeb"
    }

    init { run {
        if (data.isWebLoginValid()) {
            onSuccess()
        }
        else {
            if (data.loginServerName.isNotNullNorEmpty() && data.loginUsername.isNotNullNorEmpty() && data.loginPassword.isNotNullNorEmpty()) {
                data.app.cookieJar.clear("${data.loginServerName}.mobidziennik.pl")
                loginWithCredentials()
            }
            else {
                data.error(ApiError(TAG, ERROR_LOGIN_DATA_MISSING))
            }
        }
    }}

    private fun loginWithCredentials() {
        d(TAG, "Request: Mobidziennik/Login/Web - https://${data.loginServerName}.mobidziennik.pl/api/")

        val callback = object : TextCallbackHandler() {
            override fun onSuccess(text: String?, response: Response?) {
                if (text != "ok") {
                    when {
                        text == "Nie jestes zalogowany" -> ERROR_LOGIN_MOBIDZIENNIK_WEB_INVALID_LOGIN
                        text == "ifun" -> ERROR_LOGIN_MOBIDZIENNIK_WEB_INVALID_DEVICE
                        text == "stare haslo" -> ERROR_LOGIN_MOBIDZIENNIK_WEB_OLD_PASSWORD
                        text == "Archiwum" -> ERROR_LOGIN_MOBIDZIENNIK_WEB_ARCHIVED
                        text == "Trwają prace techniczne lub pojawił się jakiś problem" -> ERROR_LOGIN_MOBIDZIENNIK_WEB_MAINTENANCE
                        text?.contains("Uuuups... nieprawidłowy adres") == true -> ERROR_LOGIN_MOBIDZIENNIK_WEB_INVALID_ADDRESS
                        text?.contains("przerwa techniczna") == true -> ERROR_LOGIN_MOBIDZIENNIK_WEB_MAINTENANCE
                        else -> ERROR_LOGIN_MOBIDZIENNIK_WEB_OTHER
                    }.let { errorCode ->
                        data.error(ApiError(TAG, errorCode)
                                .withApiResponse(text)
                                .withResponse(response))
                        return
                    }
                }

                val cookies = data.app.cookieJar.getAll("${data.loginServerName}.mobidziennik.pl")
                val cookie = cookies.entries.firstOrNull { it.key.length > 32 }
                val sessionKey = cookie?.key
                val sessionId = cookie?.value
                if (sessionId == null) {
                    data.error(ApiError(TAG, ERROR_LOGIN_MOBIDZIENNIK_WEB_NO_SESSION_ID)
                            .withResponse(response)
                            .withApiResponse(text))
                    return
                }

                data.webSessionKey = sessionKey
                data.webSessionValue = sessionId
                data.webServerId = data.app.cookieJar.get("${data.loginServerName}.mobidziennik.pl", "SERVERID")
                data.webSessionIdExpiryTime = response.getUnixDate() + 45 * 60 /* 45min */
                onSuccess()
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                data.error(ApiError(TAG, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable))
            }
        }


        Request.builder()
                .url("https://${data.loginServerName}.mobidziennik.pl/api/")
                .userAgent(MOBIDZIENNIK_USER_AGENT)
                .contentType("application/x-www-form-urlencoded; charset=UTF-8")
                .addParameter("wersja", "20")
                .addParameter("ip", data.app.deviceId)
                .addParameter("login", data.loginUsername)
                .addParameter("haslo", data.loginPassword)
                .addParameter("token", data.app.config.sync.tokenMobidziennik)
                .addParameter("ta_api", Mobidziennik.API_KEY)
                .post()
                .callback(callback)
                .build()
                .enqueue()
    }
}

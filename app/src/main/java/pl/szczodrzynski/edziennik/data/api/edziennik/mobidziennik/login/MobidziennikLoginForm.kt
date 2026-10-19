package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.login

import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.TextCallbackHandler
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.html.HtmlParsers
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.ext.getUnixDate
import pl.szczodrzynski.edziennik.ext.isNotNullNorEmpty
import timber.log.Timber

class MobidziennikLoginForm(val data: DataMobidziennik, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "MobidziennikLoginForm"
    }

    private val domain = "${data.loginServerName}.mobidziennik.pl"
    private val loginUrl = "https://$domain/dziennik"

    init { run {
        if (data.isWebLoginValid()) {
            onSuccess()
        }
        else {
            if (data.loginServerName.isNotNullNorEmpty() && data.loginUsername.isNotNullNorEmpty() && data.loginPassword.isNotNullNorEmpty()) {
                data.app.cookieJar.clear(domain)
                openLoginPage()
            }
            else {
                data.error(ApiError(TAG, ERROR_LOGIN_DATA_MISSING))
            }
        }
    }}

    private fun openLoginPage() {
        Timber.d("Request: Mobidziennik/Login/Form - GET $loginUrl")

        val callback = object : TextCallbackHandler() {
            override fun onSuccess(text: String?, response: Response?) = loginWithCredentials()
            override fun onFailure(response: Response?, throwable: Throwable?) = loginWithCredentials()
        }

        Request.builder()
                .url(loginUrl)
                .userAgent(MOBIDZIENNIK_USER_AGENT)
                .get()
                .callback(callback)
                .build()
                .enqueue()
    }

    private fun loginWithCredentials() {
        Timber.d("Request: Mobidziennik/Login/Form - POST $loginUrl")

        val callback = object : TextCallbackHandler() {
            override fun onSuccess(text: String?, response: Response?) {
                if (text.isNullOrEmpty()) {
                    data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }

                when {
                    text.contains("Uuuups... nieprawidłowy adres") -> ERROR_LOGIN_MOBIDZIENNIK_WEB_INVALID_ADDRESS
                    text.contains("przerwa techniczna") -> ERROR_LOGIN_MOBIDZIENNIK_WEB_MAINTENANCE
                    text.contains("Trwają prace techniczne lub pojawił się jakiś problem") -> ERROR_LOGIN_MOBIDZIENNIK_WEB_MAINTENANCE
                    HtmlParsers.isLoginPage(text) -> ERROR_LOGIN_MOBIDZIENNIK_WEB_INVALID_LOGIN
                    else -> null
                }?.let { errorCode ->
                    data.error(ApiError(TAG, errorCode)
                            .withApiResponse(if (HtmlParsers.isLoginPage(text)) null else text)
                            .withResponse(response))
                    return
                }

                val cookies = data.app.cookieJar.getAll(domain)
                val cookie = cookies.entries.firstOrNull { it.key.length > 32 }
                val sessionKey = cookie?.key
                val sessionId = cookie?.value
                if (sessionId == null) {
                    data.error(ApiError(TAG, ERROR_LOGIN_MOBIDZIENNIK_WEB_NO_SESSION_ID)
                            .withResponse(response)
                            .withApiResponse("Cookies: ${cookies.keys.joinToString()}"))
                    return
                }

                data.webSessionKey = sessionKey
                data.webSessionValue = sessionId
                data.webServerId = data.app.cookieJar.get(domain, "SERVERID")
                data.webSessionIdExpiryTime = response.getUnixDate() + 45 * 60
                onSuccess()
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                data.error(ApiError(TAG, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable))
            }
        }

        Request.builder()
                .url(loginUrl)
                .userAgent(MOBIDZIENNIK_USER_AGENT)
                .contentType("application/x-www-form-urlencoded; charset=UTF-8")
                .addParameter("login", data.loginUsername)
                .addParameter("haslo", data.loginPassword)
                .post()
                .callback(callback)
                .build()
                .enqueue()
    }
}

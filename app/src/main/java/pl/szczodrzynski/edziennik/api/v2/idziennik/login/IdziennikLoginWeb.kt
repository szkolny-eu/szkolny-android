/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-26.
 */

package pl.szczodrzynski.edziennik.api.v2.idziennik.login

import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.TextCallbackHandler
import okhttp3.Cookie
import pl.szczodrzynski.edziennik.HOUR
import pl.szczodrzynski.edziennik.MINUTE
import pl.szczodrzynski.edziennik.api.v2.*
import pl.szczodrzynski.edziennik.api.v2.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.get
import pl.szczodrzynski.edziennik.getUnixDate
import pl.szczodrzynski.edziennik.utils.Utils

class IdziennikLoginWeb(val data: DataIdziennik, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "IdziennikLoginWeb"
    }

    init { run {
        if (data.isWebLoginValid()) {
            data.app.cookieJar.saveFromResponse(null, listOf(
                    Cookie.Builder()
                            .name("ASP.NET_SessionId_iDziennik")
                            .value(data.webSessionId!!)
                            .domain("iuczniowie.progman.pl")
                            .secure().httpOnly().build(),
                    Cookie.Builder()
                            .name(".ASPXAUTH")
                            .value(data.webAuth!!)
                            .domain("iuczniowie.progman.pl")
                            .secure().httpOnly().build()
            ))
            onSuccess()
        }
        else {
            data.app.cookieJar.clearForDomain("iuczniowie.progman.pl")
            if (data.webSchoolName != null && data.webUsername != null && data.webPassword != null) {
                loginWithCredentials()
            }
            else {
                data.error(ApiError(TAG, ERROR_LOGIN_DATA_MISSING))
            }
        }
    }}

    private fun loginWithCredentials() {
        Utils.d(TAG, "Request: Idziennik/Login/Web - $IDZIENNIK_WEB_URL/$IDZIENNIK_WEB_LOGIN")

        val loginCallback = object : TextCallbackHandler() {
            override fun onSuccess(text: String?, response: Response?) {
                if (text.isNullOrEmpty()) {
                    data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }

                // login succeeded: there is a start page
                if (text.contains("czyWyswietlicDostepMobilny")) {
                    val cookies = data.app.cookieJar.getForDomain("iuczniowie.progman.pl")
                    run {
                        data.webSessionId = cookies.singleOrNull { it.name() == "ASP.NET_SessionId_iDziennik" }?.value() ?: return@run ERROR_LOGIN_IDZIENNIK_WEB_NO_SESSION
                        data.webAuth = cookies.singleOrNull { it.name() == ".ASPXAUTH" }?.value() ?: return@run ERROR_LOGIN_IDZIENNIK_WEB_NO_AUTH
                        data.apiBearer = cookies.singleOrNull { it.name() == "Bearer" }?.value() ?: return@run ERROR_LOGIN_IDZIENNIK_WEB_NO_BEARER
                        data.loginExpiryTime = response.getUnixDate() + 45 * MINUTE
                        data.apiExpiryTime = response.getUnixDate() + 12 * HOUR /* actually it expires after 24 hours but I'm not sure when does the token refresh. */
                        return@run null
                    }?.let { errorCode ->
                        data.error(ApiError(TAG, errorCode)
                                .withApiResponse(text)
                                .withResponse(response))
                        return
                    }

                    onSuccess()
                    return
                }

                val errorText = Regexes.IDZIENNIK_LOGIN_ERROR.find(text)?.get(1)
                when {
                    errorText?.contains("nieprawidłową nazwę szkoły") == true -> ERROR_LOGIN_IDZIENNIK_WEB_INVALID_SCHOOL_NAME
                    errorText?.contains("nieprawidłowy login lub hasło") == true -> ERROR_LOGIN_IDZIENNIK_WEB_INVALID_LOGIN
                    text.contains("Identyfikator zgłoszenia") -> ERROR_LOGIN_IDZIENNIK_WEB_SERVER_ERROR
                    text.contains("Hasło dostępu do systemu wygasło") -> ERROR_LOGIN_IDZIENNIK_WEB_PASSWORD_CHANGE_NEEDED
                    text.contains("Trwają prace konserwacyjne") -> ERROR_LOGIN_IDZIENNIK_WEB_MAINTENANCE
                    else -> ERROR_LOGIN_IDZIENNIK_WEB_OTHER
                }.let { errorCode ->
                    data.error(ApiError(TAG, errorCode)
                            .withApiResponse(text)
                            .withResponse(response))
                    return
                }
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                data.error(ApiError(TAG, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable))
            }
        }

        val getCallback = object : TextCallbackHandler() {
            override fun onSuccess(text: String?, response: Response?) {
                Request.builder()
                        .url("$IDZIENNIK_WEB_URL/$IDZIENNIK_WEB_LOGIN")
                        .userAgent(IDZIENNIK_USER_AGENT)
                        .addHeader("Origin", "https://iuczniowie.progman.pl")
                        .addHeader("Referer", "$IDZIENNIK_WEB_URL/$IDZIENNIK_WEB_LOGIN")
                        .apply {
                            Regexes.IDZIENNIK_LOGIN_HIDDEN_FIELDS.findAll(text ?: return@apply).forEach {
                                addParameter(it[1], it[2])
                            }
                        }
                        .addParameter("ctl00\$ContentPlaceHolder\$nazwaPrzegladarki", IDZIENNIK_USER_AGENT)
                        .addParameter("ctl00\$ContentPlaceHolder\$NazwaSzkoly", data.webSchoolName)
                        .addParameter("ctl00\$ContentPlaceHolder\$UserName", data.webUsername)
                        .addParameter("ctl00\$ContentPlaceHolder\$Password", data.webPassword)
                        .addParameter("ctl00\$ContentPlaceHolder\$captcha", "")
                        .addParameter("ctl00\$ContentPlaceHolder\$Logowanie", "Zaloguj")
                        .post()
                        .allowErrorCode(502)
                        .callback(loginCallback)
                        .build()
                        .enqueue()
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                data.error(ApiError(TAG, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable))
            }
        }

        Request.builder()
                .url("$IDZIENNIK_WEB_URL/$IDZIENNIK_WEB_LOGIN")
                .userAgent(IDZIENNIK_USER_AGENT)
                .get()
                .allowErrorCode(502)
                .callback(getCallback)
                .build()
                .enqueue()
    }
}

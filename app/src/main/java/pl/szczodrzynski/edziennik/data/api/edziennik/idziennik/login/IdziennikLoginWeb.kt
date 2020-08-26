/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-26.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.login

import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.TextCallbackHandler
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.LuckyNumber
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.edziennik.utils.models.Date

class IdziennikLoginWeb(val data: DataIdziennik, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "IdziennikLoginWeb"
    }

    init { run {
        if (data.isWebLoginValid()) {
            data.app.cookieJar.set("iuczniowie.progman.pl", "ASP.NET_SessionId_iDziennik", data.webSessionId)
            data.app.cookieJar.set("iuczniowie.progman.pl", ".ASPXAUTH", data.webAuth)
            onSuccess()
        }
        else {
            data.app.cookieJar.clear("iuczniowie.progman.pl")
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
                    val cookies = data.app.cookieJar.getAll("iuczniowie.progman.pl")
                    run {
                        data.webSessionId = cookies["ASP.NET_SessionId_iDziennik"] ?: return@run ERROR_LOGIN_IDZIENNIK_WEB_NO_SESSION
                        data.webAuth = cookies[".ASPXAUTH"] ?: return@run ERROR_LOGIN_IDZIENNIK_WEB_NO_AUTH
                        data.apiBearer = cookies["Bearer"]?: return@run ERROR_LOGIN_IDZIENNIK_WEB_NO_BEARER
                        data.loginExpiryTime = response.getUnixDate() + 30 * MINUTE /* after about 40 minutes the login didn't work already */
                        data.apiExpiryTime = response.getUnixDate() + 12 * HOUR /* actually it expires after 24 hours but I'm not sure when does the token refresh. */

                        val hiddenFields = JsonObject()
                        Regexes.IDZIENNIK_LOGIN_HIDDEN_FIELDS.findAll(text).forEach {
                            hiddenFields[it[1]] = it[2]
                        }
                        data.loginStore.putLoginData("hiddenFields", hiddenFields)

                        Regexes.IDZIENNIK_WEB_SELECTED_REGISTER.find(text)?.let {
                            val registerId = it[1].toIntOrNull() ?: return@let
                            data.webSelectedRegister = registerId
                        }

                        // for profiles created after archiving
                        data.schoolYearId = Regexes.IDZIENNIK_LOGIN_FIRST_SCHOOL_YEAR.find(text)?.let {
                            it[1].toIntOrNull()
                        } ?: data.schoolYearId
                        data.profile?.studentClassName = Regexes.IDZIENNIK_LOGIN_FIRST_STUDENT.findAll(text)
                                .firstOrNull { it[1].toIntOrNull() == data.registerId }
                                ?.let { "${it[5]} ${it[6]}" } ?: data.profile?.studentClassName

                        data.profile?.let { profile ->
                            Regexes.IDZIENNIK_WEB_LUCKY_NUMBER.find(text)?.also {
                                val number = it[1].toIntOrNull() ?: return@also
                                val luckyNumberObject = LuckyNumber(
                                        profileId = data.profileId,
                                        date = Date.getToday(),
                                        number = number
                                )

                                data.luckyNumberList.add(luckyNumberObject)
                                data.metadataList.add(
                                        Metadata(
                                                profile.id,
                                                Metadata.TYPE_LUCKY_NUMBER,
                                                luckyNumberObject.date.value.toLong(),
                                                true,
                                                profile.empty
                                        ))
                            }
                        }

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

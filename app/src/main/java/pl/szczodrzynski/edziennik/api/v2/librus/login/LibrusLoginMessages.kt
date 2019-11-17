/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-20.
 */

package pl.szczodrzynski.edziennik.api.v2.librus.login

import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.body.MediaTypeUtils
import im.wangchao.mhttp.callback.TextCallbackHandler
import okhttp3.Cookie
import pl.szczodrzynski.edziennik.api.v2.*
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.getUnixDate
import pl.szczodrzynski.edziennik.utils.Utils.d
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class LibrusLoginMessages(val data: DataLibrus, val onSuccess: () -> Unit) {
    companion object {
        private const val TAG = "LoginLibrusMessages"
    }

    private val callback by lazy { object : TextCallbackHandler() {
        override fun onSuccess(text: String?, response: Response?) {
            val location = response?.headers()?.get("Location")
            when {
                location?.contains("MultiDomainLogon") == true -> loginWithSynergia(location)
                location?.contains("AutoLogon") == true -> {
                    saveSessionId(response, text)
                    onSuccess()
                }

                text?.contains("<status>ok</status>") == true -> {
                    saveSessionId(response, text)
                    onSuccess()
                }
                text?.contains("<message>Niepoprawny login i/lub hasło.</message>") == true -> data.error(TAG, ERROR_LOGIN_LIBRUS_MESSAGES_INVALID_LOGIN, response, text)
                text?.contains("stop.png") == true -> data.error(TAG, ERROR_LIBRUS_SYNERGIA_ACCESS_DENIED, response, text)
                text?.contains("eAccessDeny") == true -> data.error(TAG, ERROR_LIBRUS_MESSAGES_ACCESS_DENIED, response, text)
                text?.contains("OffLine") == true -> data.error(TAG, ERROR_LIBRUS_MESSAGES_MAINTENANCE, response, text)
                text?.contains("<status>error</status>") == true -> data.error(TAG, ERROR_LIBRUS_MESSAGES_ERROR, response, text)
                text?.contains("<type>eVarWhitThisNameNotExists</type>") == true -> data.error(TAG, ERROR_LIBRUS_MESSAGES_ACCESS_DENIED, response, text)
                text?.contains("<error>") == true -> data.error(TAG, ERROR_LIBRUS_MESSAGES_OTHER, response, text)
            }
        }

        override fun onFailure(response: Response?, throwable: Throwable?) {
            data.error(ApiError(TAG, ERROR_REQUEST_FAILURE)
                    .withResponse(response)
                    .withThrowable(throwable))
        }
    }}

    init { run {
        if (data.profile == null) {
            data.error(ApiError(TAG, ERROR_PROFILE_MISSING))
            return@run
        }

        if (data.isMessagesLoginValid()) {
            data.app.cookieJar.saveFromResponse(null, listOf(
                    Cookie.Builder()
                            .name("DZIENNIKSID")
                            .value(data.messagesSessionId!!)
                            .domain("wiadomosci.librus.pl")
                            .secure().httpOnly().build()
            ))
            onSuccess()
        }
        else {
            data.app.cookieJar.clearForDomain("wiadomosci.librus.pl")
            if (data.loginMethods.contains(LOGIN_METHOD_LIBRUS_SYNERGIA)) {
                loginWithSynergia()
            }
            else if (data.apiLogin != null && data.apiPassword != null) {
                loginWithCredentials()
            }
            else {
                data.error(ApiError(TAG, ERROR_LOGIN_DATA_MISSING))
            }
        }
    }}

    /**
     * XML (Flash messages website) login method. Uses a Synergia login and password.
     */
    private fun loginWithCredentials() {
        d(TAG, "Request: Librus/Login/Messages - $LIBRUS_MESSAGES_URL/Login")

        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.newDocument()
        val serviceElement = doc.createElement("service")
        val headerElement = doc.createElement("header")
        val dataElement = doc.createElement("data")
        val loginElement = doc.createElement("login")
        loginElement.appendChild(doc.createTextNode(data.apiLogin))
        dataElement.appendChild(loginElement)
        val passwordElement = doc.createElement("login")
        passwordElement.appendChild(doc.createTextNode(data.apiPassword))
        dataElement.appendChild(passwordElement)
        val keyStrokeElement = doc.createElement("KeyStroke")
        val keysElement = doc.createElement("Keys")
        val upElement = doc.createElement("Up")
        keysElement.appendChild(upElement)
        val downElement = doc.createElement("Down")
        keysElement.appendChild(downElement)
        keyStrokeElement.appendChild(keysElement)
        dataElement.appendChild(keyStrokeElement)
        serviceElement.appendChild(headerElement)
        serviceElement.appendChild(dataElement)
        doc.appendChild(serviceElement)
        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        val stringWriter = StringWriter()
        transformer.transform(DOMSource(doc), StreamResult(stringWriter))
        val requestXml = stringWriter.toString()

        Request.builder()
                .url("$LIBRUS_MESSAGES_URL/Login")
                .userAgent(SYNERGIA_USER_AGENT)
                .setTextBody(requestXml, MediaTypeUtils.APPLICATION_XML)
                .post()
                .callback(callback)
                .build()
                .enqueue()
    }

    /**
     * A login method using the Synergia website (/wiadomosci2 Auto Login).
     */
    private fun loginWithSynergia(url: String = "https://synergia.librus.pl/wiadomosci2") {
        d(TAG, "Request: Librus/Login/Messages - $url")

        Request.builder()
                .url(url)
                .userAgent(SYNERGIA_USER_AGENT)
                .get()
                .callback(callback)
                .withClient(data.app.httpLazy)
                .build()
                .enqueue()
    }

    private fun saveSessionId(response: Response?, text: String?) {
        var sessionId = data.app.cookieJar.getCookie("wiadomosci.librus.pl", "DZIENNIKSID")
        sessionId = sessionId?.replace("-MAINT", "") // dunno what's this
        if (sessionId == null) {
            data.error(ApiError(TAG, ERROR_LOGIN_LIBRUS_MESSAGES_NO_SESSION_ID)
                    .withResponse(response)
                    .withApiResponse(text))
            return
        }
        data.messagesSessionId = sessionId
        data.messagesSessionIdExpiryTime = response.getUnixDate() + 45 * 60 /* 45min */
    }
}
/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-24
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data

import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.body.MediaTypeUtils
import im.wangchao.mhttp.callback.TextCallbackHandler
import okhttp3.Cookie
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import pl.szczodrzynski.edziennik.api.v2.*
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.utils.Utils.d
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

open class LibrusMessages(open val data: DataLibrus) {
    companion object {
        private const val TAG = "LibrusMessages"
    }

    val profileId
        get() = data.profile?.id ?: -1

    val profile
        get() = data.profile

    fun messagesGet(tag: String, endpoint: String, method: Int = POST,
                    parameters: Map<String, Any>? = null, onSuccess: (doc: Document) -> Unit) {

        d(tag, "Request: Librus/Messages - $LIBRUS_MESSAGES_URL/$endpoint")

        val callback = object : TextCallbackHandler() {
            override fun onSuccess(text: String?, response: Response?) {
                if (text.isNullOrEmpty()) {
                    data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }

                when {
                    text.contains("<message>Niepoprawny login i/lub hasło.</message>") -> data.error(TAG, ERROR_LOGIN_LIBRUS_MESSAGES_INVALID_LOGIN, response, text)
                    text.contains("stop.png") -> data.error(TAG, ERROR_LIBRUS_SYNERGIA_ACCESS_DENIED, response, text)
                    text.contains("eAccessDeny") -> data.error(TAG, ERROR_LIBRUS_MESSAGES_ACCESS_DENIED, response, text)
                    text.contains("OffLine") -> data.error(TAG, ERROR_LIBRUS_MESSAGES_MAINTENANCE, response, text)
                    text.contains("<status>error</status>") -> data.error(TAG, ERROR_LIBRUS_MESSAGES_ERROR, response, text)
                    text.contains("<type>eVarWhitThisNameNotExists</type>") -> data.error(TAG, ERROR_LIBRUS_MESSAGES_ACCESS_DENIED, response, text)
                    text.contains("<error>") -> data.error(TAG, ERROR_LIBRUS_MESSAGES_OTHER, response, text)
                }

                try {
                    val doc = Jsoup.parse(text, "", Parser.xmlParser())
                    onSuccess(doc)
                } catch (e: Exception) {
                    data.error(ApiError(tag, EXCEPTION_LIBRUS_MESSAGES_REQUEST)
                            .withResponse(response)
                            .withThrowable(e)
                            .withApiResponse(text))
                }
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                data.error(ApiError(tag, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable))
            }
        }

        data.app.cookieJar.saveFromResponse(null, listOf(
                Cookie.Builder()
                        .name("DZIENNIKSID")
                        .value(data.messagesSessionId!!)
                        .domain("wiadomosci.librus.pl")
                        .secure().httpOnly().build()
        ))


        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = docBuilder.newDocument()
        val serviceElement = doc.createElement("service")
        val headerElement = doc.createElement("header")
        val dataElement = doc.createElement("data")
        for ((key, value) in parameters.orEmpty()) {
            val element = doc.createElement(key)
            element.appendChild(doc.createTextNode(value.toString()))
            dataElement.appendChild(element)
        }
        serviceElement.appendChild(headerElement)
        serviceElement.appendChild(dataElement)
        doc.appendChild(serviceElement)
        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
        val stringWriter = StringWriter()
        transformer.transform(DOMSource(doc), StreamResult(stringWriter))
        val requestXml = stringWriter.toString()

        /*val requestXml = xml("service") {
            "header" { }
            "data" {
                for ((key, value) in parameters.orEmpty()) {
                    key {
                        -value.toString()
                    }
                }
            }
        }.toString(PrintOptions(
                singleLineTextElements = true,
                useSelfClosingTags = true
        ))*/

        Request.builder()
                .url("$LIBRUS_MESSAGES_URL/$endpoint")
                .userAgent(SYNERGIA_USER_AGENT)
                .setTextBody(requestXml, MediaTypeUtils.APPLICATION_XML)
                .apply {
                    when (method) {
                        GET -> get()
                        POST -> post()
                    }
                }
                .callback(callback)
                .build()
                .enqueue()
    }
}

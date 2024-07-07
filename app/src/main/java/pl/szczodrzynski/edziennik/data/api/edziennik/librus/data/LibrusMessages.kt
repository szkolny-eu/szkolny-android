/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-24
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.body.MediaTypeUtils
import im.wangchao.mhttp.callback.FileCallbackHandler
import im.wangchao.mhttp.callback.JsonCallbackHandler
import im.wangchao.mhttp.callback.TextCallbackHandler
import org.json.JSONObject
import org.json.XML
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import pl.szczodrzynski.edziennik.data.api.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import timber.log.Timber
import java.io.File
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

open class LibrusMessages(open val data: DataLibrus, open val lastSync: Long?) {
    companion object {
        private const val TAG = "LibrusMessages"
    }

    val profileId
        get() = data.profile?.id ?: -1

    val profile
        get() = data.profile

    fun messagesGet(tag: String, module: String, method: Int = POST,
                    parameters: Map<String, Any>? = null, onSuccess: (doc: Document) -> Unit) {

        Timber.tag(tag).d("Request: Librus/Messages - $LIBRUS_MESSAGES_URL/$module")

        val callback = object : TextCallbackHandler() {
            override fun onSuccess(text: String?, response: Response?) {
                if (text.isNullOrEmpty()) {
                    data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }

                when {
                    text.contains("<message>Niepoprawny login i/lub hasło.</message>") -> ERROR_LOGIN_LIBRUS_MESSAGES_INVALID_LOGIN
                    text.contains("<message>Nie odnaleziono wiadomości.</message>") -> ERROR_LIBRUS_MESSAGES_NOT_FOUND
                    text.contains("stop.png") -> ERROR_LIBRUS_SYNERGIA_ACCESS_DENIED
                    text.contains("eAccessDeny") -> ERROR_LIBRUS_MESSAGES_ACCESS_DENIED
                    text.contains("OffLine") -> ERROR_LIBRUS_MESSAGES_MAINTENANCE
                    text.contains("<status>error</status>") -> ERROR_LIBRUS_MESSAGES_ERROR
                    text.contains("<type>eVarWhitThisNameNotExists</type>") -> ERROR_LIBRUS_MESSAGES_ACCESS_DENIED
                    text.contains("<error>") -> ERROR_LIBRUS_MESSAGES_OTHER
                    else -> null
                }?.let { errorCode ->
                    data.error(ApiError(tag, errorCode)
                            .withApiResponse(text)
                            .withResponse(response))
                    return
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

        data.app.cookieJar.set("wiadomosci.librus.pl", "DZIENNIKSID", data.messagesSessionId)

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

        Request.builder()
                .url("$LIBRUS_MESSAGES_URL/$module")
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

    fun messagesGetJson(tag: String, module: String, method: Int = POST,
                        parameters: Map<String, Any>? = null, onSuccess: (json: JsonObject?) -> Unit) {

        Timber.tag(tag).d("Request: Librus/Messages - $LIBRUS_MESSAGES_URL/$module")

        val callback = object : TextCallbackHandler() {
            override fun onSuccess(text: String?, response: Response?) {
                if (text.isNullOrEmpty()) {
                    data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }

                when {
                    text.contains("<message>Niepoprawny login i/lub hasło.</message>") -> ERROR_LOGIN_LIBRUS_MESSAGES_INVALID_LOGIN
                    text.contains("<message>Nie odnaleziono wiadomości.</message>") -> ERROR_LIBRUS_MESSAGES_NOT_FOUND
                    text.contains("stop.png") -> ERROR_LIBRUS_SYNERGIA_ACCESS_DENIED
                    text.contains("eAccessDeny") -> ERROR_LIBRUS_MESSAGES_ACCESS_DENIED
                    text.contains("OffLine") -> ERROR_LIBRUS_MESSAGES_MAINTENANCE
                    text.contains("<status>error</status>") -> ERROR_LIBRUS_MESSAGES_ERROR
                    text.contains("<type>eVarWhitThisNameNotExists</type>") -> ERROR_LIBRUS_MESSAGES_ACCESS_DENIED
                    text.contains("<error>") -> ERROR_LIBRUS_MESSAGES_OTHER
                    else -> null
                }?.let { errorCode ->
                    data.error(ApiError(tag, errorCode)
                            .withApiResponse(text)
                            .withResponse(response))
                    return
                }

                try {
                    val json: JSONObject? = XML.toJSONObject(text)
                    onSuccess(JsonParser().parse(json?.toString() ?: "{}")?.asJsonObject)
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

        data.app.cookieJar.set("wiadomosci.librus.pl", "DZIENNIKSID", data.messagesSessionId)

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

        Request.builder()
                .url("$LIBRUS_MESSAGES_URL/$module")
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

    fun sandboxGet(tag: String, action: String, parameters: Map<String, Any>? = null,
                   onSuccess: (json: JsonObject) -> Unit) {

        Timber.tag(tag).d("Request: Librus/Messages - $LIBRUS_SANDBOX_URL$action")

        val callback = object : JsonCallbackHandler() {
            override fun onSuccess(json: JsonObject?, response: Response?) {
                if (json == null) {
                    data.error(ApiError(TAG, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }

                try {
                    onSuccess(json)
                } catch (e: Exception) {
                    data.error(ApiError(tag, EXCEPTION_LIBRUS_MESSAGES_REQUEST)
                            .withResponse(response)
                            .withThrowable(e)
                            .withApiResponse(json))
                }
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                data.error(ApiError(tag, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable))
            }
        }

        Request.builder()
                .url("$LIBRUS_SANDBOX_URL$action")
                .userAgent(SYNERGIA_USER_AGENT)
                .apply {
                    parameters?.forEach { (k, v) ->
                        addParameter(k, v)
                    }
                }
                .post()
                .callback(callback)
                .build()
                .enqueue()
    }

    fun sandboxGetFile(tag: String, url: String, targetFile: File, onSuccess: (file: File) -> Unit,
                       method: Int = GET,
                       onProgress: (written: Long, total: Long) -> Unit) {

        Timber.tag(tag).d("Request: Librus/Messages - $url")

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
                    data.error(ApiError(tag, EXCEPTION_LIBRUS_MESSAGES_FILE_REQUEST)
                            .withResponse(response)
                            .withThrowable(e))
                }
            }

            override fun onProgress(bytesWritten: Long, bytesTotal: Long) {
                try {
                    onProgress(bytesWritten, bytesTotal)
                } catch (e: Exception) {
                    data.error(ApiError(tag, EXCEPTION_LIBRUS_MESSAGES_FILE_REQUEST)
                            .withThrowable(e))
                }
            }

            override fun onFailure(response: Response?, throwable: Throwable?) {
                data.error(ApiError(tag, ERROR_REQUEST_FAILURE)
                        .withResponse(response)
                        .withThrowable(throwable))
            }
        }

        Request.builder()
                .url(url)
                .userAgent(SYNERGIA_USER_AGENT)
                .also {
                    when (method) {
                        POST -> it.post()
                        else -> it.get()
                    }
                }
                .callback(callback)
                .build()
                .enqueue()
    }
}

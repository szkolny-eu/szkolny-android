/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-24
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data

import im.wangchao.mhttp.Request
import im.wangchao.mhttp.Response
import im.wangchao.mhttp.callback.TextCallbackHandler
import okhttp3.Cookie
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import org.redundent.kotlin.xml.xml
import pl.szczodrzynski.edziennik.api.v2.*
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.get
import pl.szczodrzynski.edziennik.utils.Utils.d

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
                    data.error(ApiError(LibrusSynergia.TAG, ERROR_RESPONSE_EMPTY)
                            .withResponse(response))
                    return
                }

                // TODO: Finish error handling

                if ("error" in text) {
                    when ("<type>(.*)</type>".toRegex().find(text)?.get(1)) {
                        "eAccessDeny" -> data.error(ApiError(tag, ERROR_LIBRUS_MESSAGES_ACCESS_DENIED)
                                .withResponse(response)
                                .withApiResponse(text))
                    }
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

        val requestXml = xml("service") {
            "data" {
                for ((key, value) in parameters.orEmpty()) {
                    key {
                        -value.toString()
                    }
                }
            }
        }

        Request.builder()
                .url("$LIBRUS_MESSAGES_URL/$endpoint")
                .userAgent(SYNERGIA_USER_AGENT)
                .setTextBody(requestXml.toString(), "application/xml")
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

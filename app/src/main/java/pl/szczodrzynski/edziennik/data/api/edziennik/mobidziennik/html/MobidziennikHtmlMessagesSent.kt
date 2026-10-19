package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.html

import org.jsoup.Jsoup
import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.MessageRecipient
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.ext.get

class MobidziennikHtmlMessagesSent(
    override val data: DataMobidziennik,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit,
) : MobidziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "MobidziennikHtmlMessagesSent"
    }

    init {
        webGet(TAG, "/dziennik/wiadomosciwyslane") { text ->
            if (text.contains("Brak wiadomości wysłanych.")) {
                finish()
                return@webGet
            }

            val dates = HtmlListDates()
            val list = Jsoup.parse(text).getElementsByClass("spis").first()?.getElementsByClass("podswietl")
            list?.forEach { item ->
                val id = item.attr("rel").toLongOrNull() ?: return@forEach

                val subject = item.select("td:eq(0)").first()?.ownText() ?: ""
                val hasAttachments = (item.select("td:eq(1)").first()?.getElementsByTag("a")?.size ?: 0) > 0

                val readByString = item.select("td:eq(4)").first()?.text() ?: ""
                val (readBy, sentTo) = Regexes.MOBIDZIENNIK_MESSAGE_SENT_READ_BY.find(readByString).let {
                    (it?.get(1)?.toIntOrNull() ?: 0) to (it?.get(2)?.toIntOrNull() ?: 0)
                }
                val readState = when (readBy) {
                    0 -> 0
                    sentTo -> 1
                    else -> -1
                }.toLong()

                val recipientNames = item.select("td:eq(2) a span").first()?.ownText()?.split(", ")
                recipientNames?.forEach { recipientName ->
                    val recipientId = data.htmlTeacher(recipientName)?.id ?: -1
                    data.messageRecipientIgnoreList.add(
                        MessageRecipient(data.profileId, recipientId, -1, readState, id)
                    )
                }

                val message = Message(
                    profileId = data.profileId,
                    id = id,
                    type = Message.TYPE_SENT,
                    subject = subject,
                    body = null,
                    senderId = null,
                    addedDate = dates.millis(item.select("td:eq(3)").first()?.text()?.trim())
                        ?: System.currentTimeMillis(),
                )
                if (hasAttachments)
                    message.hasAttachments = true

                data.messageList.add(message)
                data.setSeenMetadataList.add(
                    Metadata(data.profileId, MetadataType.MESSAGE, id, true, true)
                )
            }

            finish()
        }
    }

    private fun finish() {
        data.setSyncNext(ENDPOINT_MOBIDZIENNIK_HTML_MESSAGES_SENT, SYNC_ALWAYS)
        onSuccess(ENDPOINT_MOBIDZIENNIK_HTML_MESSAGES_SENT)
    }
}

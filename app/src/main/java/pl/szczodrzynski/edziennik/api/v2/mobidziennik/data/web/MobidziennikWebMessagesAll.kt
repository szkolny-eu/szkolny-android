/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-11.
 */

package pl.szczodrzynski.edziennik.api.v2.mobidziennik.data.web

import org.jsoup.Jsoup
import pl.szczodrzynski.edziennik.DAY
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_ALL
import pl.szczodrzynski.edziennik.api.v2.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message.TYPE_RECEIVED
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message.TYPE_SENT
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageRecipient
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.singleOrNull
import pl.szczodrzynski.edziennik.utils.models.Date

class MobidziennikWebMessagesAll(override val data: DataMobidziennik,
                                   val onSuccess: () -> Unit) : MobidziennikWeb(data)  {
    companion object {
        private const val TAG = "MobidziennikWebMessagesAll"
    }

    init {
        webGet(TAG, "/dziennik/wyszukiwarkawiadomosci?q=+") { text ->
            MobidziennikLuckyNumberExtractor(data, text)

            val doc = Jsoup.parse(text)

            val listElement = doc.getElementsByClass("spis").first()
            if (listElement == null) {
                data.setSyncNext(ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_ALL, 7*DAY)
                onSuccess()
                return@webGet
            }
            val list = listElement.getElementsByClass("podswietl")
            for (item in list) {
                val id = item.attr("rel").replace("[^\\d]".toRegex(), "").toLongOrNull() ?: continue

                val subjectEl = item.select("td:eq(0) div").first()
                val subject = subjectEl.text()

                val addedDateEl = item.select("td:eq(1)").first()
                val addedDate = Date.fromIsoHm(addedDateEl.text())

                val typeEl = item.select("td:eq(2) img").first()
                var type = TYPE_RECEIVED
                if (typeEl.outerHtml().contains("mail_send.png"))
                    type = TYPE_SENT

                val senderEl = item.select("td:eq(3) div").first()
                var senderId: Long = -1

                if (type == TYPE_RECEIVED) {
                    // search sender teacher
                    val senderName = senderEl.text()
                    senderId = data.teacherList.singleOrNull { it.fullNameLastFirst == senderName }?.id ?: -1
                    data.messageRecipientList.add(MessageRecipient(profileId, -1, id))
                } else {
                    // TYPE_SENT, so multiple recipients possible
                    val recipientNames = senderEl.text().split(", ")
                    for (recipientName in recipientNames) {
                        val recipientId = data.teacherList.singleOrNull { it.fullNameLastFirst == recipientName }?.id ?: -1
                        data.messageRecipientIgnoreList.add(MessageRecipient(profileId, recipientId, id))
                    }
                }

                val message = Message(
                        profileId,
                        id,
                        subject,
                        null,
                        type,
                        senderId,
                        -1
                )

                data.messageIgnoreList.add(message)
                data.metadataList.add(Metadata(profileId, Metadata.TYPE_MESSAGE, message.id, true, true, addedDate))
            }

            // sync every 7 days as we probably don't except more than
            // 30 received messages during a week, without any normal sync
            data.setSyncNext(ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_ALL, 7*DAY)
            onSuccess()
        }
    }
}
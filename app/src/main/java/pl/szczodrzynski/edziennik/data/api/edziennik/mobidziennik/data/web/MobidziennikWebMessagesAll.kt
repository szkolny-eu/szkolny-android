/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-11.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.web

import org.jsoup.Jsoup
import pl.szczodrzynski.edziennik.DAY
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_ALL
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Message.Companion.TYPE_RECEIVED
import pl.szczodrzynski.edziennik.data.db.entity.Message.Companion.TYPE_SENT
import pl.szczodrzynski.edziennik.data.db.entity.MessageRecipient
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.fixName
import pl.szczodrzynski.edziennik.singleOrNull
import pl.szczodrzynski.edziennik.utils.models.Date

class MobidziennikWebMessagesAll(override val data: DataMobidziennik,
                                 override val lastSync: Long?,
                                 val onSuccess: (endpointId: Int) -> Unit
) : MobidziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "MobidziennikWebMessagesAll"
    }

    init {
        webGet(TAG, "/dziennik/wyszukiwarkawiadomosci?q=+") { text ->
            MobidziennikLuckyNumberExtractor(data, text)

            val doc = Jsoup.parse(text)

            val listElement = doc.getElementsByClass("spis")?.first()
            if (listElement == null) {
                data.setSyncNext(ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_ALL, 7*DAY)
                onSuccess(ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_ALL)
                return@webGet
            }
            val list = listElement.getElementsByClass("podswietl")
            list?.forEach { item ->
                val id = item.attr("rel").replace("[^\\d]".toRegex(), "").toLongOrNull() ?: return@forEach

                val subjectEl = item.select("td:eq(0) div").first()
                val subject = subjectEl.text()

                val addedDateEl = item.select("td:eq(1)").first()
                val addedDate = Date.fromIsoHm(addedDateEl.text())

                val typeEl = item.select("td:eq(2) img").first()
                var type = TYPE_RECEIVED
                if (typeEl.outerHtml().contains("mail_send.png"))
                    type = TYPE_SENT

                val senderEl = item.select("td:eq(3) div").first()
                var senderId: Long? = null

                if (type == TYPE_RECEIVED) {
                    // search sender teacher
                    val senderName = senderEl.text().fixName()
                    senderId = data.teacherList.singleOrNull { it.fullNameLastFirst == senderName }?.id
                    data.messageRecipientList.add(MessageRecipient(profileId, -1, id))
                } else {
                    // TYPE_SENT, so multiple recipients possible
                    val recipientNames = senderEl.text().split(", ")
                    for (recipientName in recipientNames) {
                        val name = recipientName.fixName()
                        val recipientId = data.teacherList.singleOrNull { it.fullNameLastFirst == name }?.id ?: -1
                        data.messageRecipientIgnoreList.add(MessageRecipient(profileId, recipientId, id))
                    }
                }

                val message = Message(
                        profileId = profileId,
                        id = id,
                        type = type,
                        subject = subject,
                        body = null,
                        senderId = senderId
                )

                data.messageList.add(message)
                data.metadataList.add(Metadata(profileId, Metadata.TYPE_MESSAGE, message.id, true, true, addedDate))
            }

            // sync every 7 days as we probably don't expect more than
            // 30 received messages during a week, without any normal sync
            data.setSyncNext(ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_ALL, 7*DAY)
            onSuccess(ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_ALL)
        }
    }
}

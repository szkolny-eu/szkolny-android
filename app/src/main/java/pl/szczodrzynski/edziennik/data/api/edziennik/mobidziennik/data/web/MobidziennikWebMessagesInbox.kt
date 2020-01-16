/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-11.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.web

import org.jsoup.Jsoup
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_INBOX
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.MessageRecipient
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.fixName
import pl.szczodrzynski.edziennik.singleOrNull
import pl.szczodrzynski.edziennik.utils.models.Date

class MobidziennikWebMessagesInbox(override val data: DataMobidziennik,
                              val onSuccess: () -> Unit) : MobidziennikWeb(data) {
    companion object {
        private const val TAG = "MobidziennikWebMessagesInbox"
    }

    init {
        webGet(TAG, "/dziennik/wiadomosci") { text ->
            MobidziennikLuckyNumberExtractor(data, text)

            if (text.contains("Brak wiadomości odebranych.")) {
                data.setSyncNext(ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_INBOX, SYNC_ALWAYS)
                onSuccess()
                return@webGet
            }

            val doc = Jsoup.parse(text)

            val list = doc.getElementsByClass("spis").first().getElementsByClass("podswietl")
            for (item in list) {
                val id = item.attr("rel").toLongOrNull() ?: continue

                val subjectEl = item.select("td:eq(0)").first()
                var hasAttachments = false
                if (subjectEl.getElementsByTag("a").size != 0) {
                    hasAttachments = true
                }
                val subject = subjectEl.ownText()

                val addedDateEl = item.select("td:eq(1) small").first()
                val addedDate = Date.fromIsoHm(addedDateEl.text())

                val senderEl = item.select("td:eq(2)").first()
                val senderName = senderEl.ownText().fixName()
                val senderId = data.teacherList.singleOrNull { it.fullNameLastFirst == senderName }?.id ?: -1
                data.messageRecipientIgnoreList.add(MessageRecipient(profileId, -1, id))

                val isRead = item.select("td:eq(3) span").first().hasClass("wiadomosc_przeczytana")

                val message = Message(
                        profileId,
                        id,
                        subject,
                        null,
                        Message.TYPE_RECEIVED,
                        senderId,
                        -1
                )

                if (hasAttachments)
                    message.setHasAttachments()

                data.messageIgnoreList.add(message)
                data.setSeenMetadataList.add(
                        Metadata(
                                profileId,
                                Metadata.TYPE_MESSAGE,
                                message.id,
                                isRead,
                                isRead || profile?.empty ?: false,
                                addedDate
                        ))
            }

            data.setSyncNext(ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_INBOX, SYNC_ALWAYS)
            onSuccess()
        }
    }
}

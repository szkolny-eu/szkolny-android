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
                                   override val lastSync: Long?,
                                   val onSuccess: (endpointId: Int) -> Unit
) : MobidziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "MobidziennikWebMessagesInbox"
    }

    init {
        webGet(TAG, "/dziennik/wiadomosci") { text ->
            MobidziennikLuckyNumberExtractor(data, text)

            if (text.contains("Brak wiadomości odebranych.")) {
                data.setSyncNext(ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_INBOX, SYNC_ALWAYS)
                onSuccess(ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_INBOX)
                return@webGet
            }

            val doc = Jsoup.parse(text)

            val list = doc.getElementsByClass("spis").first()?.getElementsByClass("podswietl")
            list?.forEach { item ->
                val id = item.attr("rel").toLongOrNull() ?: return@forEach

                val subjectEl = item.select("td:eq(0)").first()
                var hasAttachments = false
                if (subjectEl?.getElementsByTag("a")?.size ?: 0 > 0) {
                    hasAttachments = true
                }
                val subject = subjectEl?.ownText() ?: ""

                val addedDateEl = item.select("td:eq(1) small").first()
                val addedDate = Date.fromIsoHm(addedDateEl?.text())

                val senderEl = item.select("td:eq(2)").first()
                val senderName = senderEl?.ownText().fixName()
                val senderId = data.teacherList.singleOrNull { it.fullNameLastFirst == senderName }?.id
                data.messageRecipientIgnoreList.add(MessageRecipient(profileId, -1, id))

                val isRead = item.select("td:eq(3) span").first()?.hasClass("wiadomosc_przeczytana") == true

                val message = Message(
                        profileId = profileId,
                        id = id,
                        type = Message.TYPE_RECEIVED,
                        subject = subject,
                        body = null,
                        senderId = senderId,
                        addedDate = addedDate
                )

                if (hasAttachments)
                    message.hasAttachments = true

                data.messageList.add(message)
                data.setSeenMetadataList.add(
                        Metadata(
                                profileId,
                                Metadata.TYPE_MESSAGE,
                                message.id,
                                isRead,
                                isRead || profile?.empty ?: false
                        ))
            }

            data.setSyncNext(ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_INBOX, SYNC_ALWAYS)
            onSuccess(ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_INBOX)
        }
    }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-18.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.web

import org.jsoup.Jsoup
import pl.szczodrzynski.edziennik.DAY
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_MESSAGES
import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_SENT
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.MessageRecipient
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.fixName
import pl.szczodrzynski.edziennik.get
import pl.szczodrzynski.edziennik.singleOrNull
import pl.szczodrzynski.edziennik.utils.models.Date

class MobidziennikWebMessagesSent(override val data: DataMobidziennik,
                                   override val lastSync: Long?,
                                   val onSuccess: (endpointId: Int) -> Unit
) : MobidziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "MobidziennikWebMessagesSent"
    }

    init {
        webGet(TAG, "/dziennik/wiadomosciwyslane") { text ->
            MobidziennikLuckyNumberExtractor(data, text)

            if (text.contains("Brak wiadomości wysłanych.")) {
                data.setSyncNext(ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_SENT, SYNC_ALWAYS)
                onSuccess(ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_SENT)
                return@webGet
            }

            val doc = Jsoup.parse(text)

            val list = doc.getElementsByClass("spis")?.first()?.getElementsByClass("podswietl")
            list?.forEach { item ->
                val id = item.attr("rel").toLongOrNull() ?: return@forEach

                val subjectEl = item.select("td:eq(0)").first()
                var hasAttachments = false
                if (subjectEl.getElementsByTag("a").size != 0) {
                    hasAttachments = true
                }
                val subject = subjectEl.ownText()

                val readByString = item.select("td:eq(2)").first().text()
                val (readBy, sentTo) = Regexes.MOBIDZIENNIK_MESSAGE_SENT_READ_BY.find(readByString).let {
                    (it?.get(1)?.toIntOrNull() ?: 0) to (it?.get(2)?.toIntOrNull() ?: 0)
                }

                val recipientEl = item.select("td:eq(1) a span").first()
                val recipientNames = recipientEl.ownText().split(", ")
                val readState = when (readBy) {
                    0 -> 0
                    sentTo -> 1
                    else -> -1
                }.toLong()
                for (recipientName in recipientNames) {
                    val name = recipientName.fixName()
                    val recipientId = data.teacherList.singleOrNull { it.fullNameLastFirst == name }?.id ?: -1
                    data.messageRecipientIgnoreList.add(MessageRecipient(profileId, recipientId, -1, readState, id))
                }

                val addedDateEl = item.select("td:eq(3) small").first()
                val addedDate = Date.fromIsoHm(addedDateEl.text())

                val message = Message(
                        profileId = profileId,
                        id = id,
                        type = Message.TYPE_SENT,
                        subject = subject,
                        body = null,
                        senderId = null,
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
                                true,
                                true
                        ))
            }

            data.setSyncNext(ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_SENT, 1*DAY, DRAWER_ITEM_MESSAGES)
            onSuccess(ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_SENT)
        }
    }
}

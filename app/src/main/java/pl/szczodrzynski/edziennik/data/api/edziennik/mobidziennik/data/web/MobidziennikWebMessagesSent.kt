/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-18.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.web

import org.jsoup.Jsoup
import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_SENT
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.MessageRecipient
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.enums.FeatureType
import pl.szczodrzynski.edziennik.data.enums.MetadataType
import pl.szczodrzynski.edziennik.ext.DAY
import pl.szczodrzynski.edziennik.ext.fixName
import pl.szczodrzynski.edziennik.ext.get
import pl.szczodrzynski.edziennik.ext.singleOrNull
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

            val today = Date.getToday()
            var currentYear = today.year
            var currentMonth = today.month

            val list = doc.getElementsByClass("spis").first()?.getElementsByClass("podswietl")
            list?.forEach { item ->
                val id = item.attr("rel").toLongOrNull() ?: return@forEach

                val subjectEl = item.select("td:eq(0)").first()
                val subject = subjectEl?.ownText() ?: ""

                val attachmentsEl = item.select("td:eq(1)").first()
                var hasAttachments = false
                if (attachmentsEl?.getElementsByTag("a")?.size ?: 0 > 0) {
                    hasAttachments = true
                }

                val readByString = item.select("td:eq(4)").first()?.text() ?: ""
                val (readBy, sentTo) = Regexes.MOBIDZIENNIK_MESSAGE_SENT_READ_BY.find(readByString).let {
                    (it?.get(1)?.toIntOrNull() ?: 0) to (it?.get(2)?.toIntOrNull() ?: 0)
                }

                val recipientEl = item.select("td:eq(2) a span").first()
                val recipientNames = recipientEl?.ownText()?.split(", ")
                val readState = when (readBy) {
                    0 -> 0
                    sentTo -> 1
                    else -> -1
                }.toLong()
                recipientNames?.forEach { recipientName ->
                    val name = recipientName.fixName()
                    val recipientId = data.teacherList.singleOrNull { it.fullNameLastFirst == name }?.id ?: -1
                    data.messageRecipientIgnoreList.add(MessageRecipient(profileId, recipientId, -1, readState, id))
                }

                val addedDateEl = item.select("td:eq(3)").first()
                val (date, time) = data.parseDateTime(addedDateEl?.text()?.trim() ?: "")
                if (date.month > currentMonth) {
                    currentYear--
                }
                currentMonth = date.month
                date.year = currentYear

                val message = Message(
                        profileId = profileId,
                        id = id,
                        type = Message.TYPE_SENT,
                        subject = subject,
                        body = null,
                        senderId = null,
                        addedDate = date.combineWith(time)
                )

                if (hasAttachments)
                    message.hasAttachments = true

                data.messageList.add(message)
                data.setSeenMetadataList.add(
                        Metadata(
                                profileId,
                                MetadataType.MESSAGE,
                                message.id,
                                true,
                                true
                        ))
            }

            data.setSyncNext(ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_SENT, 1* DAY, FeatureType.MESSAGES_SENT)
            onSuccess(ENDPOINT_MOBIDZIENNIK_WEB_MESSAGES_SENT)
        }
    }
}

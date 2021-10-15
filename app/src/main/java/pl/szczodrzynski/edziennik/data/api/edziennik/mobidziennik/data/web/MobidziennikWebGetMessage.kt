/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-18.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.web

import org.greenrobot.eventbus.EventBus
import org.jsoup.Jsoup
import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.api.events.MessageGetEvent
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.data.db.full.MessageRecipientFull
import pl.szczodrzynski.edziennik.fixName
import pl.szczodrzynski.edziennik.get
import pl.szczodrzynski.edziennik.singleOrNull
import pl.szczodrzynski.edziennik.utils.Utils.monthFromName
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class MobidziennikWebGetMessage(override val data: DataMobidziennik,
                                private val message: MessageFull,
                                val onSuccess: () -> Unit
) : MobidziennikWeb(data, null) {
    companion object {
        private const val TAG = "MobidziennikWebGetMessage"
    }

    init {
        val typeUrl = if (message.isSent)
            "wiadwyslana"
        else
            "wiadodebrana"
        webGet(TAG, "/dziennik/$typeUrl/?id=${message.id}") { text ->
            MobidziennikLuckyNumberExtractor(data, text)

            val messageRecipientList = mutableListOf<MessageRecipientFull>()

            val doc = Jsoup.parse(text)

            val content = doc.select("#content").first() ?: return@webGet

            val body = content.select(".wiadomosc_tresc").first()
            val bodyHtml = body?.html() ?: ""

            if (message.isReceived) {
                var readDate = System.currentTimeMillis()
                Regexes.MOBIDZIENNIK_MESSAGE_READ_DATE.find(bodyHtml)?.let {
                    val date = Date(
                            it[3].toIntOrNull() ?: 2019,
                            monthFromName(it[2]),
                            it[1].toIntOrNull() ?: 1
                    )
                    val time = Time.fromH_m_s(
                            it[4] // TODO blank string safety
                    )
                    readDate = date.combineWith(time)
                }

                val recipient = MessageRecipientFull(
                        profileId = profileId,
                        id = -1,
                        messageId = message.id,
                        readDate = readDate
                )

                recipient.fullName = profile?.accountName ?: profile?.studentNameLong ?: ""

                messageRecipientList.add(recipient)
            } else {
                message.senderId = null

                content.select("table.spis tr:has(td)").forEach { recipientEl ->
                    val senderEl = recipientEl.select("td:eq(1)").first() ?: return@forEach
                    val senderName = senderEl.text().fixName()

                    val teacher = data.teacherList.singleOrNull { it.fullNameLastFirst == senderName }
                    val receiverId = teacher?.id ?: -1

                    var readDate = 0L
                    val isReadEl = recipientEl.select("td:eq(4)").first() ?: return@forEach
                    if (isReadEl.html().contains("tak")) {
                        val readDateEl = recipientEl.select("td:eq(5) small").first() ?: return@forEach
                        Regexes.MOBIDZIENNIK_MESSAGE_SENT_READ_DATE.find(readDateEl.ownText())?.let {
                            val date = Date(
                                it[3].toIntOrNull() ?: 2019,
                                monthFromName(it[2]),
                                it[1].toIntOrNull() ?: 1
                            )
                            val time = Time.fromH_m_s(
                                it[4] // TODO blank string safety
                            )
                            readDate = date.combineWith(time)
                        }
                    }

                    val recipient = MessageRecipientFull(
                        profileId = profileId,
                        id = receiverId,
                        messageId = message.id,
                        readDate = readDate
                    )

                    recipient.fullName = teacher?.fullName ?: "?"

                    messageRecipientList.add(recipient)
                }
            }

            // this line removes the sender and read date details
            body?.select("div")?.remove()

            // this needs to be at the end
            message.apply {
                this.body = body?.html()

                clearAttachments()
                Regexes.MOBIDZIENNIK_WEB_ATTACHMENT.findAll(text).forEach { match ->
                    val attachmentId = match[2].toLong()
                    val attachmentName = match[3]
                    var size = match[4].toFloatOrNull() ?: -1f
                    when (match[5]) {
                        "K" -> size *= 1024f
                        "M" -> size *= 1024f * 1024f
                        "G" -> size *= 1024f * 1024f * 1024f
                    }
                    message.addAttachment(attachmentId, attachmentName, size.toLong())
                }
            }

            if (!message.seen) { // TODO discover why this monstrosity instead of MetadataDao.setSeen
                data.setSeenMetadataList.add(Metadata(
                        message.profileId,
                        Metadata.TYPE_MESSAGE,
                        message.id,
                        true,
                        true
                ))
            }

            message.recipients = messageRecipientList
            data.messageRecipientList.addAll(messageRecipientList)

            data.messageList.add(message)
            data.messageListReplace = true

            EventBus.getDefault().postSticky(MessageGetEvent(message))
            onSuccess()
        }
    }
}

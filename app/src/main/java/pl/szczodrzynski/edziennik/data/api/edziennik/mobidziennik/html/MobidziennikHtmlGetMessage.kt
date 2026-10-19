package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.html

import org.greenrobot.eventbus.EventBus
import org.jsoup.Jsoup
import org.jsoup.nodes.Entities
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.api.events.MessageGetEvent
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.data.db.full.MessageRecipientFull

class MobidziennikHtmlGetMessage(
    override val data: DataMobidziennik,
    private val message: MessageFull,
    val onSuccess: () -> Unit,
) : MobidziennikWeb(data, null) {
    companion object {
        private const val TAG = "MobidziennikHtmlGetMessage"
    }

    init {
        val page = if (message.isSent) "wiadwyslana" else "wiadodebrana"
        webGet(TAG, "/dziennik/$page?id=${message.id}") { text ->
            val parsed = HtmlParsers.parseMessage(text, message.id.toString())

            val recipients = if (message.isReceived) {
                val me = MessageRecipientFull(
                    profileId = profileId,
                    id = -1,
                    messageId = message.id,
                    readDate = data.htmlDateTimeMillis(parsed.przeczytano) ?: System.currentTimeMillis(),
                )
                me.fullName = profile?.accountName ?: profile?.studentNameLong ?: ""
                mutableListOf(me)
            } else {
                message.senderId = null
                sentRecipients(text)
            }

            message.apply {
                body = parsed.tresc.split("\n").joinToString("<br>") { Entities.escape(it) }
                clearAttachments()
                htmlAttachments(text).forEach { addAttachment(it.id, it.name, it.size) }
            }

            if (!message.seen) {
                data.setSeenMetadataList.add(
                    Metadata(message.profileId, MetadataType.MESSAGE, message.id, true, true)
                )
            }

            message.recipients = recipients
            data.messageRecipientList.addAll(recipients)
            data.messageList.add(message)
            data.messageListReplace = true

            EventBus.getDefault().postSticky(MessageGetEvent(message))
            onSuccess()
        }
    }

    private fun sentRecipients(text: String): MutableList<MessageRecipientFull> {
        val out = mutableListOf<MessageRecipientFull>()
        val content = Jsoup.parse(text).select("#content").first() ?: return out
        content.select("table.spis tr:has(td)").forEach { row ->
            val name = row.select("td:eq(1)").first()?.text() ?: return@forEach
            val teacher = data.htmlTeacher(name)

            var readDate = 0L
            val isRead = row.select("td:eq(4)").first() ?: return@forEach
            if (isRead.html().contains("tak")) {
                val readDateEl = row.select("td:eq(5) small").first() ?: return@forEach
                readDate = data.htmlDateTimeMillis(readDateEl.ownText()) ?: 0L
            }

            val recipient = MessageRecipientFull(
                profileId = profileId,
                id = teacher?.id ?: -1,
                messageId = message.id,
                readDate = readDate,
            )
            recipient.fullName = teacher?.fullName ?: "?"
            out += recipient
        }
        return out
    }
}

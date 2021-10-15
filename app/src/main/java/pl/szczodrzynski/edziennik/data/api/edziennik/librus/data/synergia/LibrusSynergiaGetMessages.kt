package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.synergia

import org.jsoup.Jsoup
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.ERROR_NOT_IMPLEMENTED
import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusSynergia
import pl.szczodrzynski.edziennik.data.db.entity.*
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.edziennik.utils.models.Date

class LibrusSynergiaGetMessages(override val data: DataLibrus,
                                override val lastSync: Long?,
                                private val type: Int = Message.TYPE_RECEIVED,
                                archived: Boolean = false,
                                val onSuccess: (Int) -> Unit) : LibrusSynergia(data, lastSync) {
    companion object {
        const val TAG = "LibrusSynergiaGetMessages"
    }

    init {
        val endpoint = when (type) {
            Message.TYPE_RECEIVED -> "wiadomosci/5"
            Message.TYPE_SENT -> "wiadomosci/6"
            else -> null
        }
        val endpointId = when (type) {
            Message.TYPE_RECEIVED -> ENDPOINT_LIBRUS_SYNERGIA_MESSAGES_RECEIVED
            else -> ENDPOINT_LIBRUS_SYNERGIA_MESSAGES_SENT
        }

        if (endpoint != null) {
            synergiaGet(TAG, endpoint) { text ->
                val doc = Jsoup.parse(text)

                fun getRecipientId(name: String): Long = data.teacherList.singleOrNull {
                    it.fullNameLastFirst == name
                }?.id ?: run {
                    val teacherObject = Teacher(
                        profileId,
                        -1 * Utils.crc16(name.swapFirstLastName().toByteArray()).toLong(),
                        name.splitName()?.second!!,
                        name.splitName()?.first!!
                    )
                    data.teacherList.put(teacherObject.id, teacherObject)
                    teacherObject.id
                }

                doc.select(".decorated.stretch tbody > tr").forEach { messageElement ->
                    val url = messageElement.select("a").first()?.attr("href") ?: return@forEach
                    val id = Regexes.LIBRUS_MESSAGE_ID.find(url)?.get(1)?.toLong() ?: return@forEach
                    val subject = messageElement.child(3).text()
                    val sentDate = Date.fromIso(messageElement.child(4).text())
                    val recipientName = messageElement.child(2).text().split('(')[0].fixName()
                    val recipientId = getRecipientId(recipientName)
                    val read = messageElement.child(2).attr("style").isNullOrBlank()

                    val senderId = when (type) {
                        Message.TYPE_RECEIVED -> recipientId
                        else -> null
                    }

                    val receiverId = when (type) {
                        Message.TYPE_RECEIVED -> -1
                        else -> recipientId
                    }

                    val notified = when (type) {
                        Message.TYPE_SENT -> true
                        else -> read || profile?.empty ?: false
                    }

                    val messageObject = Message(
                            profileId = profileId,
                            id = id,
                            type = type,
                            subject = subject,
                            body = null,
                            senderId = senderId,
                            addedDate = sentDate
                    )

                    val messageRecipientObject = MessageRecipient(
                            profileId,
                            receiverId,
                            -1,
                            if (read) 1 else 0,
                            id
                    )

                    messageObject.hasAttachments = !messageElement.child(1).select("img").isEmpty()

                    data.messageList.add(messageObject)
                    data.messageRecipientList.add(messageRecipientObject)
                    data.setSeenMetadataList.add(Metadata(
                            profileId,
                            Metadata.TYPE_MESSAGE,
                            id,
                            notified,
                            notified
                    ))
                }

                when (type) {
                    Message.TYPE_RECEIVED -> data.setSyncNext(ENDPOINT_LIBRUS_MESSAGES_RECEIVED, SYNC_ALWAYS)
                    Message.TYPE_SENT -> data.setSyncNext(ENDPOINT_LIBRUS_MESSAGES_SENT, DAY, MainActivity.DRAWER_ITEM_MESSAGES)
                }
                onSuccess(endpointId)
            }
        } else {
            data.error(TAG, ERROR_NOT_IMPLEMENTED)
            onSuccess(endpointId)
        }
    }
}

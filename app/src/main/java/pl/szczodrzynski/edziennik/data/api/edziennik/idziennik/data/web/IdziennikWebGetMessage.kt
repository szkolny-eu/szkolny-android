/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-28
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.web

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.IDZIENNIK_WEB_GET_MESSAGE
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.IdziennikWeb
import pl.szczodrzynski.edziennik.data.api.events.MessageGetEvent
import pl.szczodrzynski.edziennik.data.db.entity.Message.TYPE_RECEIVED
import pl.szczodrzynski.edziennik.data.db.entity.Message.TYPE_SENT
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.data.db.full.MessageRecipientFull
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.utils.models.Date

class IdziennikWebGetMessage(
        override val data: DataIdziennik,
        private val message: MessageFull,
        val onSuccess: () -> Unit
) : IdziennikWeb(data) {
    companion object {
        const val TAG = "IdziennikWebGetMessage"
    }

    init { data.profile?.also { profile ->
        val metaPattern = "\\[META:([A-z0-9]+);([0-9-]+)]".toRegex()
        val meta = metaPattern.find(message.body!!)
        val messageIdString = meta?.get(1) ?: ""

        webApiGet(TAG, IDZIENNIK_WEB_GET_MESSAGE, parameters = mapOf(
                "idWiadomosci" to messageIdString,
                "typWiadomosci" to if (message.type == TYPE_SENT) 1 else 0
        )) { json ->
            json.getJsonObject("d")?.getJsonObject("Wiadomosc")?.also {
                val id = it.getLong("_recordId")
                message.body = message.body?.replace(metaPattern, "[META:$messageIdString;$id]")

                message.clearAttachments()
                it.getJsonArray("ListaZal")?.asJsonObjectList()?.forEach { attachment ->
                    message.addAttachment(
                            attachment.getLong("Id") ?: return@forEach,
                            attachment.getString("Nazwa") ?: return@forEach,
                            -1
                    )
                }

                message.recipients?.clear()
                when (message.type) {
                    TYPE_RECEIVED -> {
                        val recipientObject = MessageRecipientFull(profileId, -1, message.id)

                        val readDateString = it.getString("DataOdczytania")
                        recipientObject.readDate = if (readDateString.isNullOrBlank()) System.currentTimeMillis()
                        else Date.fromIso(readDateString)

                        recipientObject.fullName = profile.accountName ?: profile.studentNameLong

                        data.messageRecipientList.add(recipientObject)
                        message.addRecipient(recipientObject)
                    }

                    TYPE_SENT -> {
                        it.getJsonArray("ListaOdbiorcow")?.asJsonObjectList()?.forEach { recipient ->
                            val recipientName = recipient.getString("NazwaOdbiorcy") ?: return@forEach
                            val teacher = data.getTeacherByLastFirst(recipientName)

                            val recipientObject = MessageRecipientFull(profileId, teacher.id, message.id)

                            recipientObject.readDate = recipient.getLong("Status") ?: return@forEach
                            recipientObject.fullName = teacher.fullName

                            data.messageRecipientList.add(recipientObject)
                            message.addRecipient(recipientObject)
                        }
                    }
                }

                if (!message.seen) {
                    message.seen = true

                    data.setSeenMetadataList.add(Metadata(
                            profileId,
                            Metadata.TYPE_MESSAGE,
                            message.id,
                            message.seen,
                            message.notified,
                            message.addedDate
                    ))
                }

                EventBus.getDefault().postSticky(MessageGetEvent(message))

                data.messageList.add(message)
                onSuccess()
            }
        }
    } ?: onSuccess() }
}

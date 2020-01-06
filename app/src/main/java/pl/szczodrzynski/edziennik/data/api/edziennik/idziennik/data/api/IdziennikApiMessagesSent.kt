/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-30.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.api

import com.google.gson.JsonArray
import pl.szczodrzynski.edziennik.DAY
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_MESSAGES
import pl.szczodrzynski.edziennik.asJsonObjectList
import pl.szczodrzynski.edziennik.data.api.IDZIENNIK_API_MESSAGES_SENT
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.ENDPOINT_IDZIENNIK_API_MESSAGES_SENT
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.IdziennikApi
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Message.TYPE_SENT
import pl.szczodrzynski.edziennik.data.db.entity.MessageRecipient
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.utils.Utils.crc32
import pl.szczodrzynski.edziennik.utils.models.Date

class IdziennikApiMessagesSent(override val data: DataIdziennik,
                                val onSuccess: () -> Unit) : IdziennikApi(data) {
    companion object {
        private const val TAG = "IdziennikApiMessagesSent"
    }

    init {
        apiGet(TAG, IDZIENNIK_API_MESSAGES_SENT) { json ->
            if (json !is JsonArray) {
                onSuccess()
                return@apiGet
            }

            json.asJsonObjectList()?.forEach { jMessage ->
                val messageIdStr = jMessage.get("id").asString
                val messageId = crc32((messageIdStr + "1").toByteArray())

                val subject = jMessage.get("tytul").asString

                var body = "[META:$messageIdStr;-1]"
                body += jMessage.get("tresc").asString.replace("\n".toRegex(), "<br>")

                val sentDate = Date.fromIso(jMessage.get("dataWyslania").asString)

                val message = Message(
                        profileId,
                        messageId,
                        subject,
                        body,
                        TYPE_SENT,
                        -1,
                        -1
                )

                for (recipientEl in jMessage.getAsJsonArray("odbiorcy")) {
                    val recipient = recipientEl.asJsonObject
                    var firstName = recipient.get("imie").asString
                    var lastName = recipient.get("nazwisko").asString
                    if (firstName.isEmpty() || lastName.isEmpty()) {
                        firstName = "usunięty"
                        lastName = "użytkownik"
                    }
                    val rTeacher = data.getTeacher(firstName, lastName)
                    rTeacher.loginId = /*recipient.get("id").asString + ":" + */recipient.get("usr").asString

                    val messageRecipient = MessageRecipient(
                            profileId,
                            rTeacher.id,
                            -1,
                            -1,
                            /*messageId*/ messageId
                    )
                    data.messageRecipientIgnoreList.add(messageRecipient)
                }

                data.messageIgnoreList.add(message)
                data.metadataList.add(Metadata(profileId, Metadata.TYPE_MESSAGE, message.id, true, true, sentDate))
            }

            data.setSyncNext(ENDPOINT_IDZIENNIK_API_MESSAGES_SENT, DAY, DRAWER_ITEM_MESSAGES)
            onSuccess()
        }
    }
}

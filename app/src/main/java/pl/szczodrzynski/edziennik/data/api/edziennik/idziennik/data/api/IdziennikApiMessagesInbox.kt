/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-30.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.api

import com.google.gson.JsonArray
import pl.szczodrzynski.edziennik.asJsonObjectList
import pl.szczodrzynski.edziennik.data.api.IDZIENNIK_API_MESSAGES_INBOX
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.ENDPOINT_IDZIENNIK_API_MESSAGES_INBOX
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.IdziennikApi
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message.TYPE_DELETED
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message.TYPE_RECEIVED
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageRecipient
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher
import pl.szczodrzynski.edziennik.getBoolean
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.utils.Utils.crc32
import pl.szczodrzynski.edziennik.utils.models.Date

class IdziennikApiMessagesInbox(override val data: DataIdziennik,
                                  val onSuccess: () -> Unit) : IdziennikApi(data) {
    companion object {
        private const val TAG = "IdziennikApiMessagesInbox"
    }

    init {
        apiGet(TAG, IDZIENNIK_API_MESSAGES_INBOX) { json ->
            if (json !is JsonArray) {
                onSuccess()
                return@apiGet
            }

            json.asJsonObjectList()?.forEach { jMessage ->
                val subject = jMessage.getString("tytul")
                if (subject?.contains("(") == true && subject.startsWith("iDziennik - "))
                    return@forEach
                if (subject?.startsWith("Uwaga dla ucznia (klasa:") == true)
                    return@forEach

                val messageIdStr = jMessage.getString("id")
                val messageId = crc32((messageIdStr + "0").toByteArray())

                var body = "[META:$messageIdStr;-1]"
                body += jMessage.getString("tresc")?.replace("\n".toRegex(), "<br>")

                val readDate = if (jMessage.getBoolean("odczytana") == true) Date.fromIso(jMessage.getString("wersjaRekordu")) else 0
                val sentDate = Date.fromIso(jMessage.getString("dataWyslania"))

                val sender = jMessage.getAsJsonObject("nadawca")
                var firstName = sender.getString("imie")
                var lastName = sender.getString("nazwisko")
                if (firstName.isNullOrEmpty() || lastName.isNullOrEmpty()) {
                    firstName = "usunięty"
                    lastName = "użytkownik"
                }
                val rTeacher = data.getTeacher(
                        firstName,
                        lastName
                )
                rTeacher.loginId = /*sender.getString("id") + ":" + */sender.getString("usr")
                rTeacher.setTeacherType(Teacher.TYPE_OTHER)

                val message = Message(
                        profileId,
                        messageId,
                        subject,
                        body,
                        if (jMessage.getBoolean("rekordUsuniety") == true) TYPE_DELETED else TYPE_RECEIVED,
                        rTeacher.id,
                        -1
                )

                val messageRecipient = MessageRecipient(
                        profileId,
                        -1 /* me */,
                        -1,
                        readDate,
                        /*messageId*/ messageId
                )

                data.messageIgnoreList.add(message)
                data.messageRecipientList.add(messageRecipient)
                data.setSeenMetadataList.add(Metadata(
                        profileId,
                        Metadata.TYPE_MESSAGE,
                        message.id,
                        readDate > 0,
                        readDate > 0 || profile?.empty ?: false,
                        sentDate
                ))
            }

            data.setSyncNext(ENDPOINT_IDZIENNIK_API_MESSAGES_INBOX, SYNC_ALWAYS)
            onSuccess()
        }
    }
}

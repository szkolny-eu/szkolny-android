/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-01
 */

package pl.szczodrzynski.edziennik.api.v2.vulcan.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.v2.VULCAN_API_ENDPOINT_MESSAGES_RECEIVED
import pl.szczodrzynski.edziennik.api.v2.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.api.v2.vulcan.ENDPOINT_VULCAN_API_MESSAGES_INBOX
import pl.szczodrzynski.edziennik.api.v2.vulcan.data.VulcanApi
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageRecipient
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata

class VulcanApiMessagesInbox(override val data: DataVulcan, val onSuccess: () -> Unit) : VulcanApi(data) {
    companion object {
        const val TAG = "VulcanApiMessagesInbox"
    }

    init {
        apiGet(TAG, VULCAN_API_ENDPOINT_MESSAGES_RECEIVED, parameters = mapOf(
                "DataPoczatkowa" to data.syncStartDate.inUnix,
                "DataKoncowa" to data.syncEndDate.inUnix,
                "LoginId" to data.studentLoginId,
                "IdUczen" to data.studentId
        )) { json, _ ->
            json.getJsonArray("Data").asJsonObjectList()?.forEach { message ->
                val id = message.getLong("WiadomoscId") ?: return@forEach
                val subject = message.getString("Tytul") ?: ""
                val body = message.getString("Tresc") ?: ""

                val senderLoginId = message.getString("NadawcaId") ?: return@forEach
                val senderId = data.teacherList
                        .singleOrNull { it.loginId == senderLoginId }?.id ?: return@forEach

                val addedDate = message.getLong("DataWyslaniaUnixEpoch")?.let { it * 1000 } ?: -1
                val readDate = message.getLong("DataPrzeczytaniaUnixEpoch")?.let { it * 1000 } ?: -1

                val messageObject = Message(
                        profileId,
                        id,
                        subject,
                        body,
                        Message.TYPE_RECEIVED,
                        senderId,
                        -1
                )

                val messageRecipientObject = MessageRecipient(
                        profileId,
                        -1,
                        -1,
                        readDate,
                        id
                )

                data.messageList.add(messageObject)
                data.messageRecipientList.add(messageRecipientObject)
                data.metadataList.add(Metadata(
                        profileId,
                        Metadata.TYPE_MESSAGE,
                        id,
                        readDate > 0,
                        readDate > 0,
                        addedDate
                ))
            }

            data.setSyncNext(ENDPOINT_VULCAN_API_MESSAGES_INBOX, SYNC_ALWAYS)
            onSuccess()
        }
    }
}

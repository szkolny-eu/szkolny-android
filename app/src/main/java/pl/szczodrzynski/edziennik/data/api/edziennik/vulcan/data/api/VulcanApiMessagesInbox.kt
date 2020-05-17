/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-01
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.VULCAN_API_ENDPOINT_MESSAGES_RECEIVED
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_API_MESSAGES_INBOX
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanApi
import pl.szczodrzynski.edziennik.data.db.entity.*
import pl.szczodrzynski.edziennik.data.db.entity.Message.Companion.TYPE_RECEIVED
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.edziennik.utils.models.Date
import kotlin.text.replace

class VulcanApiMessagesInbox(override val data: DataVulcan,
                             override val lastSync: Long?,
                             val onSuccess: (endpointId: Int) -> Unit
) : VulcanApi(data, lastSync) {
    companion object {
        const val TAG = "VulcanApiMessagesInbox"
    }

    init {
        data.profile?.also { profile ->

            val startDate = when (profile.empty) {
                true -> profile.getSemesterStart(profile.currentSemester).inUnix
                else -> Date.getToday().stepForward(0, -2, 0).inUnix
            }
            val endDate = Date.getToday().stepForward(0, 1, 0).inUnix

            apiGet(TAG, VULCAN_API_ENDPOINT_MESSAGES_RECEIVED, parameters = mapOf(
                    "DataPoczatkowa" to startDate,
                    "DataKoncowa" to endDate,
                    "LoginId" to data.studentLoginId,
                    "IdUczen" to data.studentId
            )) { json, _ ->
                json.getJsonArray("Data")?.asJsonObjectList()?.forEach { message ->
                    val id = message.getLong("WiadomoscId") ?: return@forEach
                    val subject = message.getString("Tytul") ?: ""
                    val body = message.getString("Tresc") ?: ""

                    val senderLoginId = message.getString("NadawcaId") ?: return@forEach
                    val senderId = data.teacherList.singleOrNull { it.loginId == senderLoginId }?.id ?: {

                        val senderName = message.getString("Nadawca") ?: ""

                        senderName.splitName()?.let { (senderLastName, senderFirstName) ->
                            val teacherObject = Teacher(
                                    profileId,
                                    -1 * Utils.crc16(senderName.toByteArray()).toLong(),
                                    senderFirstName,
                                    senderLastName,
                                    senderLoginId
                            )
                            data.teacherList.put(teacherObject.id, teacherObject)
                            teacherObject.id
                        }
                    }.invoke()

                    val sentDate = message.getLong("DataWyslaniaUnixEpoch")?.let { it * 1000 }
                            ?: -1
                    val readDate = message.getLong("DataPrzeczytaniaUnixEpoch")?.let { it * 1000 }
                            ?: -1

                    val messageObject = Message(
                            profileId = profileId,
                            id = id,
                            type = TYPE_RECEIVED,
                            subject = subject,
                            body = body.replace("\n", "<br>"),
                            senderId = senderId,
                            addedDate = sentDate
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
                    data.setSeenMetadataList.add(Metadata(
                            profileId,
                            Metadata.TYPE_MESSAGE,
                            id,
                            readDate > 0,
                            readDate > 0
                    ))
                }

                data.setSyncNext(ENDPOINT_VULCAN_API_MESSAGES_INBOX, SYNC_ALWAYS)
                onSuccess(ENDPOINT_VULCAN_API_MESSAGES_INBOX)
            }
        } ?: onSuccess(ENDPOINT_VULCAN_API_MESSAGES_INBOX)
    }
}

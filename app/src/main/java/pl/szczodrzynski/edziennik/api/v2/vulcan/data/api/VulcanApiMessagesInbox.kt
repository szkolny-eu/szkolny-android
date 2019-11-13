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
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message.TYPE_RECEIVED
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageRecipient
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.edziennik.utils.models.Date

class VulcanApiMessagesInbox(override val data: DataVulcan, val onSuccess: () -> Unit) : VulcanApi(data) {
    companion object {
        const val TAG = "VulcanApiMessagesInbox"
    }

    init {
        data.profile?.also { profile ->

            val startDate: String = when (profile.empty) {
                true -> profile.getSemesterStart(profile.currentSemester).stringY_m_d
                else -> Date.getToday().stepForward(0, -1, 0).stringY_m_d
            }
            val endDate: String = profile.getSemesterEnd(profile.currentSemester).stringY_m_d

            apiGet(TAG, VULCAN_API_ENDPOINT_MESSAGES_RECEIVED, parameters = mapOf(
                    "DataPoczatkowa" to startDate,
                    "DataKoncowa" to endDate,
                    "LoginId" to data.studentLoginId,
                    "IdUczen" to data.studentId
            )) { json, _ ->
                json.getJsonArray("Data").asJsonObjectList()?.forEach { message ->
                    val id = message.getLong("WiadomoscId") ?: return@forEach
                    val subject = message.getString("Tytul") ?: ""
                    val body = message.getString("Tresc") ?: ""

                    val senderLoginId = message.getString("NadawcaId") ?: return@forEach
                    val senderId = data.teacherList
                            .singleOrNull { it.loginId == senderLoginId }?.id ?: {

                        val senderName = message.getString("Nadawca") ?: ""

                        senderName.getLastFirstName()?.let { (senderLastName, senderFirstName) ->
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
                    }.invoke() ?: -1

                    val sentDate = message.getLong("DataWyslaniaUnixEpoch")?.let { it * 1000 }
                            ?: -1
                    val readDate = message.getLong("DataPrzeczytaniaUnixEpoch")?.let { it * 1000 }
                            ?: -1

                    val messageObject = Message(
                            profileId,
                            id,
                            subject,
                            body,
                            TYPE_RECEIVED,
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

                    data.messageIgnoreList.add(messageObject)
                    data.messageRecipientList.add(messageRecipientObject)
                    data.metadataList.add(Metadata(
                            profileId,
                            Metadata.TYPE_MESSAGE,
                            id,
                            readDate > 0,
                            readDate > 0,
                            sentDate
                    ))
                }

                data.setSyncNext(ENDPOINT_VULCAN_API_MESSAGES_INBOX, SYNC_ALWAYS)
                onSuccess()
            }
        } ?: onSuccess()
    }
}

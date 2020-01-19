/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-5
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_MESSAGES
import pl.szczodrzynski.edziennik.data.api.VULCAN_API_ENDPOINT_MESSAGES_SENT
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_API_MESSAGES_SENT
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanApi
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Message.TYPE_SENT
import pl.szczodrzynski.edziennik.data.db.entity.MessageRecipient
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.edziennik.utils.models.Date
import kotlin.text.replace

class VulcanApiMessagesSent(override val data: DataVulcan, val onSuccess: () -> Unit) : VulcanApi(data) {
    companion object {
        const val TAG = "VulcanApiMessagesSent"
    }

    init {
        data.profile?.also { profile ->

            val startDate = when (profile.empty) {
                true -> profile.getSemesterStart(profile.currentSemester).inUnix
                else -> Date.getToday().stepForward(0, -2, 0).inUnix
            }
            val endDate = Date.getToday().stepForward(0, 1, 0).inUnix

            apiGet(TAG, VULCAN_API_ENDPOINT_MESSAGES_SENT, parameters = mapOf(
                    "DataPoczatkowa" to startDate,
                    "DataKoncowa" to endDate,
                    "LoginId" to data.studentLoginId,
                    "IdUczen" to data.studentId
            )) { json, _ ->
                json.getJsonArray("Data")?.asJsonObjectList()?.forEach { message ->
                    val id = message.getLong("WiadomoscId") ?: return@forEach
                    val subject = message.getString("Tytul") ?: ""
                    val body = message.getString("Tresc") ?: ""
                    val readBy = message.getInt("Przeczytane") ?: 0
                    val unreadBy = message.getInt("Nieprzeczytane") ?: 0
                    val sentDate = message.getLong("DataWyslaniaUnixEpoch")?.let { it * 1000 } ?: -1

                    message.getJsonArray("Adresaci")?.asJsonObjectList()
                            ?.onEach { receiver ->

                                val receiverLoginId = receiver.getString("LoginId")
                                        ?: return@onEach
                                val receiverId = data.teacherList.singleOrNull { it.loginId == receiverLoginId }?.id
                                        ?: {
                                            val receiverName = receiver.getString("Nazwa") ?: ""

                                            receiverName.splitName()?.let { (receiverLastName, receiverFirstName) ->
                                                val teacherObject = Teacher(
                                                        profileId,
                                                        -1 * Utils.crc16(receiverName.toByteArray()).toLong(),
                                                        receiverFirstName,
                                                        receiverLastName,
                                                        receiverLoginId
                                                )
                                                data.teacherList.put(teacherObject.id, teacherObject)
                                                teacherObject.id
                                            }
                                        }.invoke() ?: -1

                                val readDate: Long = when (readBy) {
                                    0 -> 0
                                    else -> when (unreadBy) {
                                        0 -> 1
                                        else -> -1
                                    }
                                }

                                val messageRecipientObject = MessageRecipient(
                                        profileId,
                                        receiverId,
                                        -1,
                                        readDate,
                                        id
                                )

                                data.messageRecipientList.add(messageRecipientObject)
                            }

                    val messageObject = Message(
                            profileId,
                            id,
                            subject,
                            body.replace("\n", "<br>"),
                            TYPE_SENT,
                            -1,
                            -1
                    )

                    data.messageIgnoreList.add(messageObject)
                    data.setSeenMetadataList.add(Metadata(
                            profileId,
                            Metadata.TYPE_MESSAGE,
                            id,
                            true,
                            true,
                            sentDate
                    ))
                }

                data.setSyncNext(ENDPOINT_VULCAN_API_MESSAGES_SENT, 1 * DAY, DRAWER_ITEM_MESSAGES)
                onSuccess()
            }
        }
    }
}

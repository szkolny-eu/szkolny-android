package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe

import androidx.core.util.set
import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_MESSAGES
import pl.szczodrzynski.edziennik.data.api.VULCAN_HEBE_ENDPOINT_MESSAGES
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_HEBE_MESSAGES_INBOX
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_HEBE_MESSAGES_SENT
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanHebe
import pl.szczodrzynski.edziennik.data.db.entity.*
import pl.szczodrzynski.edziennik.data.db.entity.Message.Companion.TYPE_DELETED
import pl.szczodrzynski.edziennik.data.db.entity.Message.Companion.TYPE_RECEIVED
import pl.szczodrzynski.edziennik.data.db.entity.Message.Companion.TYPE_SENT
import pl.szczodrzynski.navlib.crc16
import kotlin.text.replace

class VulcanHebeMessages(
    override val data: DataVulcan,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit
) : VulcanHebe(data, lastSync) {
    companion object {
        const val TAG = "VulcanHebeMessagesInbox"
    }

    private fun getPersonId(json: JsonObject): Long {
        val senderLoginId = json.getInt("LoginId") ?: return -1
        /*if (senderLoginId == data.studentLoginId)
            return -1*/

        val senderName = json.getString("Address") ?: return -1
        val senderNameSplit = senderName.splitName()
        val senderLoginIdStr = senderLoginId.toString()
        val teacher = data.teacherList.singleOrNull { it.loginId == senderLoginIdStr }
            ?: Teacher(
                profileId,
                -1 * crc16(senderName).toLong(),
                senderNameSplit?.second ?: "",
                senderNameSplit?.first ?: "",
                senderLoginIdStr
            ).also {
                it.setTeacherType(Teacher.TYPE_OTHER)
                data.teacherList[it.id] = it
            }
        return teacher.id
    }

    fun getMessages(messageType: Int) {
        val folder = when (messageType) {
            TYPE_RECEIVED -> 1
            TYPE_SENT -> 2
            TYPE_DELETED -> 3
            else -> 1
        }
        val endpointId = when (messageType) {
            TYPE_RECEIVED -> ENDPOINT_VULCAN_HEBE_MESSAGES_INBOX
            TYPE_SENT -> ENDPOINT_VULCAN_HEBE_MESSAGES_SENT
            else -> ENDPOINT_VULCAN_HEBE_MESSAGES_INBOX
        }
        apiGetList(
            TAG,
            VULCAN_HEBE_ENDPOINT_MESSAGES,
            HebeFilterType.BY_PERSON,
            folder = folder,
            lastSync = lastSync
        ) { list, _ ->
            list.forEach { message ->
                val id = message.getLong("Id") ?: return@forEach
                val subject = message.getString("Subject") ?: return@forEach
                val body = message.getString("Content") ?: return@forEach

                val sender = message.getJsonObject("Sender") ?: return@forEach

                val sentDate = getDateTime(message, "DateSent")
                val readDate = getDateTime(message, "DateRead", default = 0)

                val messageObject = Message(
                    profileId = profileId,
                    id = id,
                    type = messageType,
                    subject = subject,
                    body = body.replace("\n", "<br>"),
                    senderId = if (messageType == TYPE_RECEIVED) getPersonId(sender) else null,
                    addedDate = sentDate
                )

                val receivers = message.getJsonArray("Receiver")
                    ?.asJsonObjectList()
                    ?: return@forEach
                val receiverReadDate =
                    if (receivers.size == 1) readDate
                    else -1

                for (receiver in receivers) {
                    val messageRecipientObject = MessageRecipient(
                        profileId,
                        if (messageType == TYPE_SENT) getPersonId(receiver) else -1,
                        -1,
                        receiverReadDate,
                        id
                    )
                    data.messageRecipientList.add(messageRecipientObject)
                }

                data.messageList.add(messageObject)
                data.setSeenMetadataList.add(
                    Metadata(
                        profileId,
                        Metadata.TYPE_MESSAGE,
                        id,
                        readDate > 0 || messageType == TYPE_SENT,
                        readDate > 0 || messageType == TYPE_SENT
                    )
                )
            }

            data.setSyncNext(
                endpointId,
                if (messageType == TYPE_RECEIVED) SYNC_ALWAYS else 1 * DAY,
                if (messageType == TYPE_RECEIVED) null else DRAWER_ITEM_MESSAGES
            )
            onSuccess(endpointId)
        }
    }
}

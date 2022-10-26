/*
 * Copyright (c) Kuba Szczodrzyński 2021-2-21.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.hebe

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.data.api.VULCAN_HEBE_ENDPOINT_MESSAGEBOX_STATUS
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanHebe
import pl.szczodrzynski.edziennik.data.api.events.MessageGetEvent
import pl.szczodrzynski.edziennik.data.db.entity.MessageRecipient
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.ext.JsonObject

class VulcanHebeMessagesChangeStatus(
    override val data: DataVulcan,
    private val messageObject: MessageFull,
    val onSuccess: () -> Unit
) : VulcanHebe(data, null) {
    companion object {
        const val TAG = "VulcanHebeMessagesChangeStatus"
    }

    init { let {
        val messageKey = messageObject.body?.let { data.parseMessageMeta(it) }?.get("uuid") ?: run {
            EventBus.getDefault().postSticky(MessageGetEvent(messageObject))
            onSuccess()
            return@let
        }

        apiPost(
            TAG,
            VULCAN_HEBE_ENDPOINT_MESSAGEBOX_STATUS,
            payload = JsonObject(
                "BoxKey" to data.messageBoxKey,
                "MessageKey" to messageKey,
                "Status" to 1
            )
        ) { _: Boolean, _ ->

            if (!messageObject.seen) {
                data.setSeenMetadataList.add(
                    Metadata(
                        profileId,
                        MetadataType.MESSAGE,
                        messageObject.id,
                        true,
                        true
                    )
                )
                messageObject.seen = true
            }

            if (!messageObject.isSent) {
                val messageRecipientObject = MessageRecipient(
                    profileId,
                    -1,
                    -1,
                    System.currentTimeMillis(),
                    messageObject.id
                )
                data.messageRecipientList.add(messageRecipientObject)
            }

            EventBus.getDefault().postSticky(MessageGetEvent(messageObject))
            onSuccess()
        }
    }}
}

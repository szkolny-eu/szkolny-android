/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-7.
 */

package pl.szczodrzynski.edziennik.utils.managers

import android.os.Bundle
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorRes
import com.mikepenz.iconics.view.IconicsImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.navlib.colorAttr

class MessageManager(private val app: App) {

    suspend fun getMessage(profileId: Int, args: Bundle?): MessageFull? {
        val id = args?.getLong("messageId") ?: return null
        val json = args.getString("message")
        val addedDate = args.getLong("sentDate")
        return getMessage(profileId, id, json, addedDate)
    }

    suspend fun getMessage(
        profileId: Int,
        id: Long,
        json: String?,
        sentDate: Long = 0L
    ): MessageFull? {
        val message = if (json != null) {
            app.gson.fromJson(json, MessageFull::class.java)?.also {
                if (sentDate > 0L) {
                    it.addedDate = sentDate
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                app.db.messageDao().getByIdNow(profileId, id)
            }
        } ?: return null

        // make recipients ID-unique
        // this helps when multiple profiles receive the same message
        // (there are multiple -1 recipients for the same message ID)
        val recipientsDistinct = message.recipients?.distinctBy { it.id } ?: return null
        message.recipients?.clear()
        message.recipients?.addAll(recipientsDistinct)

        // load recipients for sent messages
        val teachers = withContext(Dispatchers.IO) {
            app.db.teacherDao().getAllNow(profileId)
        }

        message.recipients?.forEach { recipient ->
            // store the account name as a recipient
            if (recipient.id == -1L)
                recipient.fullName = app.profile.accountName ?: app.profile.studentNameLong

            // lookup a teacher by the recipient ID
            if (recipient.fullName == null)
                recipient.fullName = teachers.firstOrNull { it.id == recipient.id }?.fullName ?: ""

            // unset the readByEveryone flag
            if (recipient.readDate < 1 && message.type == Message.TYPE_SENT)
                message.readByEveryone = false
        }

        // store the account name as sender for sent messages
        if (message.type == Message.TYPE_SENT && message.senderName == null) {
            message.senderName = app.profile.accountName ?: app.profile.studentNameLong
        }

        // set the message as seen
        if (message.body != null && !message.seen) {
            app.db.metadataDao().setSeen(profileId, message, true)
        }
        //msg.recipients = app.db.messageRecipientDao().getAllByMessageId(msg.profileId, msg.id)

        return message
    }

    fun setStarIcon(image: IconicsImageView, message: Message) {
        if (message.isStarred) {
            image.icon?.colorRes = R.color.md_amber_500
            image.icon?.icon = CommunityMaterial.Icon3.cmd_star
        } else {
            image.icon?.colorAttr(image.context, android.R.attr.textColorSecondary)
            image.icon?.icon = CommunityMaterial.Icon3.cmd_star_outline
        }
    }

    suspend fun starMessage(message: Message, isStarred: Boolean) {
        message.isStarred = isStarred
        withContext(Dispatchers.Default) {
            app.db.messageDao().replace(message)
        }
    }

    suspend fun markAsDeleted(message: Message) {
        message.type = Message.TYPE_DELETED
        withContext(Dispatchers.Default) {
            app.db.messageDao().replace(message)
        }
    }
}

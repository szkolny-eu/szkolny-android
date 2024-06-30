/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-7.
 */

package pl.szczodrzynski.edziennik.utils.managers

import android.content.Context
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.widget.EditText
import com.hootsuite.nachos.NachoTextView
import com.hootsuite.nachos.chip.ChipInfo
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.colorRes
import com.mikepenz.iconics.view.IconicsImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.MessageRecipient
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.enums.MetadataType
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.ext.appendSpan
import pl.szczodrzynski.edziennik.ext.appendText
import pl.szczodrzynski.edziennik.ext.fixName
import pl.szczodrzynski.edziennik.ext.setText
import pl.szczodrzynski.edziennik.ui.messages.MessagesUtils
import pl.szczodrzynski.edziennik.utils.TextInputKeyboardEdit
import pl.szczodrzynski.edziennik.utils.html.BetterHtml
import pl.szczodrzynski.edziennik.utils.managers.TextStylingManager.HtmlMode.ORIGINAL
import pl.szczodrzynski.edziennik.utils.managers.TextStylingManager.StylingConfig
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.edziennik.utils.span.BoldSpan
import pl.szczodrzynski.edziennik.utils.span.ItalicSpan
import pl.szczodrzynski.navlib.colorAttr

class MessageManager(private val app: App) {

    class UIConfig(
        val context: Context,
        val recipients: NachoTextView,
        val subject: EditText,
        val body: TextInputKeyboardEdit,
        val teachers: List<Teacher>,
        val greetingOnCompose: Boolean,
        val greetingOnReply: Boolean,
        val greetingOnForward: Boolean,
        val greetingText: String,
    )

    private val textStylingManager
        get() = app.textStylingManager

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
                withContext(Dispatchers.IO) {
                    it.recipients = app.db.messageRecipientDao().getAllByMessageId(profileId, it.id)
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                app.db.messageDao().getByIdNow(profileId, id)
            }
        } ?: return null

        // this helps when multiple profiles receive the same message
        // (there are multiple -1 recipients for the same message ID)
        val recipientsDistinct = message.recipients?.filter { it.profileId == profileId } ?: return null
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
            if (recipient.readDate < 1 && message.isSent)
                message.readByEveryone = false
        }

        // store the account name as sender for sent messages
        if (message.isSent && message.senderName == null) {
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

    suspend fun deleteDraft(profileId: Int, messageId: Long) {
        withContext(Dispatchers.Default) {
            app.db.messageRecipientDao().clearFor(profileId, messageId)
            app.db.messageDao().delete(profileId, messageId)
            app.db.metadataDao().delete(profileId, MetadataType.MESSAGE, messageId)
        }
    }

    suspend fun saveAsDraft(config: UIConfig, stylingConfig: StylingConfig, profileId: Int, messageId: Long?) {
        val teachers = config.recipients.allChips.mapNotNull { it.data as? Teacher }
        val subject = config.subject.text?.toString() ?: ""
        val body = textStylingManager.getHtmlText(stylingConfig, htmlMode = ORIGINAL)

        withContext(Dispatchers.Default) {
            if (messageId != null) {
                app.db.messageRecipientDao().clearFor(profileId, messageId)
            }

            val message = Message(
                profileId = profileId,
                id = messageId ?: System.currentTimeMillis(),
                type = Message.TYPE_DRAFT,
                subject = subject,
                body = body,
                senderId = -1L,
                addedDate = System.currentTimeMillis(),
            )
            val metadata = Metadata(profileId, MetadataType.MESSAGE, message.id, true, true)

            val recipients = teachers.map {
                MessageRecipient(profileId, it.id, message.id)
            }

            app.db.messageDao().replace(message)
            app.db.messageRecipientDao().addAll(recipients)
            app.db.metadataDao().add(metadata)
        }
    }

    fun fillWithBundle(config: UIConfig, args: Bundle?): Message? {
        args ?: return null
        val messageJson = args.getString("message")
        val teacherId = args.getLong("messageRecipientId")
        val subject = args.getString("messageSubject")
        val payloadType = args.getString("type")

        if (config.greetingOnCompose)
            config.body.setText(config.greetingText)
        if (subject != null)
            config.subject.setText(subject)

        val message = if (messageJson != null)
            app.gson.fromJson(messageJson, MessageFull::class.java)
        else null

        when {
            message != null && message.isDraft -> {
                fillWithDraftMessage(config, message)
            }
            message != null -> {
                fillWithMessage(config, message, payloadType)
            }
            teacherId != 0L -> {
                fillWithRecipientIds(config, teacherId)
            }
        }

        return message
    }

    private fun createRecipientChips(config: UIConfig, vararg teacherIds: Long?): List<ChipInfo> {
        return teacherIds.mapNotNull { teacherId ->
            val teacher = config.teachers.firstOrNull { it.id == teacherId } ?: return@mapNotNull null
            teacher.image = MessagesUtils.getProfileImage(
                diameterDp = 48,
                textSizeBigDp = 24,
                textSizeMediumDp = 16,
                textSizeSmallDp = 12,
                count = 1,
                teacher.fullName
            )
            ChipInfo(teacher.fullName, teacher)
        }
    }

    private fun fillWithRecipientIds(config: UIConfig, vararg teacherIds: Long?) {
        config.recipients.addTextWithChips(createRecipientChips(config, *teacherIds))
    }

    private fun fillWithMessage(config: UIConfig, message: MessageFull, payloadType: String?) {
        val spanned = SpannableStringBuilder()

        val dateString = config.context.getString(
            R.string.messages_reply_date_time_format,
            Date.fromMillis(message.addedDate).formattedStringShort,
            Time.fromMillis(message.addedDate).stringHM,
        )
        // add original message info
        spanned.appendText("W dniu ")
        spanned.appendSpan(dateString, ItalicSpan(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spanned.appendText(", ")
        spanned.appendSpan(message.senderName.fixName(), ItalicSpan(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spanned.appendText(" napisał(a):")
        spanned.setSpan(BoldSpan(), 0, spanned.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spanned.appendText("\n\n")

        val greeting = when (payloadType) {
            "reply" -> {
                config.subject.setText(R.string.messages_compose_subject_reply_format, message.subject)
                if (config.greetingOnReply)
                    config.greetingText
                else null
            }
            "forward" -> {
                config.subject.setText(R.string.messages_compose_subject_forward_format, message.subject)
                if (config.greetingOnForward)
                    config.greetingText
                else null
            }
            else -> null
        }

        if (greeting == null) {
            spanned.replace(0, 0, "\n\n")
        } else {
            spanned.replace(0, 0, "$greeting\n\n\n")
        }

        val body = message.body ?: config.context.getString(R.string.messages_compose_body_load_failed)
        spanned.appendText(BetterHtml.fromHtml(config.context, body))

        fillWithRecipientIds(config, message.senderId)
        config.body.text = spanned
    }

    private fun fillWithDraftMessage(config: UIConfig, message: MessageFull) {
        val recipientIds = message.recipients?.map { it.id }?.toTypedArray() ?: emptyArray()
        fillWithRecipientIds(config, *recipientIds)

        config.subject.setText(message.subject)

        val body = message.body ?: config.context.getString(R.string.messages_compose_body_load_failed)
        config.body.setText(BetterHtml.fromHtml(config.context, body))
    }
}

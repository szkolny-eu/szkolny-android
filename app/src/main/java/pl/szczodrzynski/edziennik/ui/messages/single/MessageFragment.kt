/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-12.
 */

package pl.szczodrzynski.edziennik.ui.messages.single

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.events.MessageGetEvent
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.data.enums.LoginType
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.databinding.MessageFragmentBinding
import pl.szczodrzynski.edziennik.ext.Bundle
import pl.szczodrzynski.edziennik.ext.attachToastHint
import pl.szczodrzynski.edziennik.ext.get
import pl.szczodrzynski.edziennik.ext.isNotNullNorEmpty
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ui.base.dialog.SimpleDialog
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.ui.dialogs.settings.MessagesConfigDialog
import pl.szczodrzynski.edziennik.ui.messages.MessagesUtils
import pl.szczodrzynski.edziennik.ui.messages.list.MessagesFragment
import pl.szczodrzynski.edziennik.ui.notes.setupNotesButton
import pl.szczodrzynski.edziennik.utils.Anim
import pl.szczodrzynski.edziennik.utils.BetterLink
import pl.szczodrzynski.edziennik.utils.html.BetterHtml
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem
import kotlin.math.min

class MessageFragment : BaseFragment<MessageFragmentBinding, MainActivity>(
    inflater = MessageFragmentBinding::inflate,
) {

    override fun getBottomSheetItems() = listOf(
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.menu_messages_config)
            .withIcon(CommunityMaterial.Icon.cmd_cog_outline)
            .withOnClickListener {
                activity.bottomSheet.close()
                MessagesConfigDialog(activity, false, null, null).show()
            }
    )

    private val manager
        get() = app.messageManager
    private lateinit var message: MessageFull

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        b.closeButton.onClick { activity.navigateUp() }

        // click to expand subject and sender
        b.subject.onClick {
            it.maxLines = if (it.maxLines == 30) 2 else 30
        }
        b.senderContainer.onClick {
            b.sender.maxLines = if (b.sender.maxLines == 30) 2 else 30
        }

        b.messageStar.onClick {
            launch {
                manager.starMessage(message, !message.isStarred)
                manager.setStarIcon(b.messageStar, message)
            }
        }
        b.messageStar.attachToastHint(R.string.hint_message_star)

        b.replyButton.onClick {
            activity.navigate(navTarget = NavTarget.MESSAGE_COMPOSE, args = Bundle(
                    "message" to app.gson.toJson(message),
                    "type" to "reply"
            ))
        }
        b.forwardButton.onClick {
            activity.navigate(navTarget = NavTarget.MESSAGE_COMPOSE, args = Bundle(
                    "message" to app.gson.toJson(message),
                    "type" to "forward"
            ))
        }
        b.deleteButton.onClick {
            SimpleDialog<Unit>(activity) {
                title(R.string.messages_delete_confirmation)
                message(R.string.messages_delete_confirmation_text)
                positive(R.string.ok) {
                    manager.markAsDeleted(message)
                    Toast.makeText(
                        activity,
                        "Wiadomość przeniesiona do usuniętych",
                        Toast.LENGTH_SHORT
                    ).show()
                    activity.navigateUp()
                }
                negative(R.string.cancel)
            }.show()
        }
        b.downloadButton.isVisible = App.devMode
        b.downloadButton.onClick {
            EdziennikTask.messageGet(App.profileId, message).enqueue(activity)
        }

        message = manager.getMessage(App.profileId, arguments) ?: run {
            activity.navigateUp()
            return
        }
        b.subject.text = message.subject
        checkMessage()
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onMessageGetEvent(event: MessageGetEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        // TODO remove this: message = event.message
        showMessage()
    }

    private fun checkMessage() {
        if (message.body == null) {
            EdziennikTask.messageGet(App.profileId, message).enqueue(activity)
            return
        }

        if (app.profile.loginStoreType == LoginType.IDZIENNIK) {
            val meta = "\\[META:([A-z0-9]+);([0-9-]+)]".toRegex().find(message.body!!)
            val messageIdBefore = meta?.get(2)?.toLong() ?: -1

            if (messageIdBefore == -1L) {
                EdziennikTask.messageGet(App.profileId, message).enqueue(activity)
                return
            }
        }

        if (app.data.messagesConfig.needsReadStatus) {
            // vulcan: change message status or download attachments
            if ((message.isReceived || message.isDeleted) && !message.seen || message.attachmentIds == null) {
                EdziennikTask.messageGet(App.profileId, message).enqueue(activity)
                return
            }
        }
        else if (!message.readByEveryone) {
            // if a sent msg is not read by everyone, download it again to check the read status
            EdziennikTask.messageGet(App.profileId, message).enqueue(activity)
            return
        }

        showMessage()
    }

    private fun showMessage() {
        b.body.text = MessagesUtils.htmlToSpannable(activity, message.body.toString())
        b.date.text = getString(R.string.messages_date_time_format, Date.fromMillis(message.addedDate).formattedStringShort, Time.fromMillis(message.addedDate).stringHM)

        val messageInfo = MessagesUtils.getMessageInfo(app, message, 40, 20, 14, 10)
        b.profileBackground.setImageBitmap(messageInfo.profileImage)
        b.sender.text = messageInfo.profileName

        b.subject.text = message.subject

        manager.setStarIcon(b.messageStar, message)

        b.replyButton.isVisible = message.isReceived || message.isDeleted
        b.deleteButton.isVisible = message.isReceived

        val messageRecipients = StringBuilder("<ul>")
        message.recipients?.forEach { recipient ->
            when (recipient.readDate) {
                -1L -> messageRecipients.append(getString(
                        R.string.messages_recipients_list_unknown_state_format,
                        recipient.fullName
                ))
                0L -> messageRecipients.append(getString(
                        R.string.messages_recipients_list_unread_format,
                        recipient.fullName
                ))
                1L -> messageRecipients.append(getString(
                        R.string.messages_recipients_list_read_unknown_date_format,
                        recipient.fullName
                ))
                else -> messageRecipients.append(getString(
                        R.string.messages_recipients_list_read_format,
                        recipient.fullName,
                        Date.fromMillis(recipient.readDate).formattedString,
                        Time.fromMillis(recipient.readDate).stringHM
                ))
            }
        }
        messageRecipients.append("</ul>")
        b.recipients.text = BetterHtml.fromHtml(activity, messageRecipients)

        showAttachments()

        BetterLink.attach(b.subject)
        BetterLink.attach(b.body)

        b.progress.visibility = View.GONE
        Anim.fadeIn(b.content, 200, null)
        MessagesFragment.pageSelection = min(message.type, 1)

        b.notesButton.setupNotesButton(
            activity = activity,
            owner = message,
            onShowListener = null,
            onDismissListener = null,
        )
    }

    private fun showAttachments() {
        if (message.attachmentIds.isNullOrEmpty() || message.attachmentNames.isNullOrEmpty()) {
            b.attachmentsTitle.isVisible = false
            b.attachmentsFragment.isVisible = false
        }
        else {
            b.attachmentsTitle.isVisible = true
            b.attachmentsFragment.isVisible = true
            b.attachmentsFragment.init(Bundle().also {
                it.putInt("profileId", message.profileId)
                it.putLongArray("attachmentIds", message.attachmentIds!!.toLongArray())
                it.putStringArray("attachmentNames", message.attachmentNames!!.toTypedArray())
                if (message.attachmentSizes.isNotNullNorEmpty())
                    it.putLongArray("attachmentSizes", message.attachmentSizes!!.toLongArray())
            }, owner = message)
        }
    }
}

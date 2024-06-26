/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-12.
 */

package pl.szczodrzynski.edziennik.ui.messages.single

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.sizeDp
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.events.MessageGetEvent
import pl.szczodrzynski.edziennik.data.enums.LoginType
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.databinding.MessageFragmentBinding
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.data.enums.NavTarget
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
import pl.szczodrzynski.navlib.colorAttr
import kotlin.coroutines.CoroutineContext
import kotlin.math.min

class MessageFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "MessageFragment"
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: MessageFragmentBinding

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private val manager
        get() = app.messageManager
    private lateinit var message: MessageFull

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = MessageFragmentBinding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded) return

        activity.bottomSheet.prependItem(
            BottomSheetPrimaryItem(true)
                .withTitle(R.string.menu_messages_config)
                .withIcon(CommunityMaterial.Icon.cmd_cog_outline)
                .withOnClickListener {
                    activity.bottomSheet.close()
                    MessagesConfigDialog(activity, false, null, null).show()
                }
        )

        b.closeButton.onClick { activity.navigateUp() }

        // click to expand subject and sender
        b.subject.onClick {
            it.maxLines = if (it.maxLines == 30) 2 else 30
        }
        b.senderContainer.onClick {
            b.sender.maxLines = if (b.sender.maxLines == 30) 2 else 30
        }
        // TODO bring back iconics to reply/forward buttons - add modern icons to SzkolnyFont

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
            MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.messages_delete_confirmation)
                    .setMessage(R.string.messages_delete_confirmation_text)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        launch {
                            manager.markAsDeleted(message)
                            Toast.makeText(activity, "Wiadomość przeniesiona do usuniętych", Toast.LENGTH_SHORT).show()
                            activity.navigateUp()
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        }
        b.downloadButton.isVisible = App.devMode
        b.downloadButton.onClick {
            EdziennikTask.messageGet(App.profileId, message).enqueue(activity)
        }

        launch {
            message = manager.getMessage(App.profileId, arguments) ?: run {
                activity.navigateUp()
                return@launch
            }
            b.subject.text = message.subject
            checkMessage()
        }
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

    override fun onStart() {
        EventBus.getDefault().register(this)
        super.onStart()
    }
    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this)
    }
    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}

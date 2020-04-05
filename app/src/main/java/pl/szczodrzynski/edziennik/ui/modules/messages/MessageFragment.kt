/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-12.
 */

package pl.szczodrzynski.edziennik.ui.modules.messages

import android.os.Bundle
import android.text.Html
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
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore.Companion.LOGIN_TYPE_IDZIENNIK
import pl.szczodrzynski.edziennik.data.db.entity.Message.Companion.TYPE_DELETED
import pl.szczodrzynski.edziennik.data.db.entity.Message.Companion.TYPE_RECEIVED
import pl.szczodrzynski.edziennik.data.db.entity.Message.Companion.TYPE_SENT
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.databinding.MessageFragmentBinding
import pl.szczodrzynski.edziennik.utils.Anim
import pl.szczodrzynski.edziennik.utils.BetterLink
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
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

        b.closeButton.setImageDrawable(
                IconicsDrawable(activity, CommunityMaterial.Icon2.cmd_window_close)
                        .colorAttr(activity, android.R.attr.textColorSecondary)
                        .sizeDp(12)
        )
        b.closeButton.setOnClickListener { activity.navigateUp() }

        // click to expand subject and sender
        b.subject.onClick {
            it.maxLines = if (it.maxLines == 30) 2 else 30
        }
        b.sender.onClick {
            it.maxLines = if (it.maxLines == 30) 2 else 30
        }

        b.replyButton.onClick {
            activity.loadTarget(MainActivity.TARGET_MESSAGES_COMPOSE, Bundle(
                    "message" to app.gson.toJson(message),
                    "type" to "reply"
            ))
        }
        b.forwardButton.onClick {
            activity.loadTarget(MainActivity.TARGET_MESSAGES_COMPOSE, Bundle(
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
                            message.type = TYPE_DELETED
                            withContext(Dispatchers.Default) {
                                app.db.messageDao().replace(message)
                            }
                            Toast.makeText(activity, "Wiadomość przeniesiona do usuniętych", Toast.LENGTH_SHORT).show()
                            activity.navigateUp()
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        }

        launch {

            val messageString = arguments?.getString("message")
            val messageId = arguments?.getLong("messageId")
            if (messageId == null) {
                activity.navigateUp()
                return@launch
            }

            val msg = withContext(Dispatchers.Default) {

                val msg =
                        if (messageString != null)
                            app.gson.fromJson(messageString, MessageFull::class.java)?.also {
                                if (arguments?.getLong("sentDate") ?: 0L > 0L)
                                    it.addedDate = arguments?.getLong("sentDate") ?: 0L
                            }
                        else
                            app.db.messageDao().getByIdNow(App.profileId, messageId)

                // load recipients in sent messages
                val teachers by lazy { app.db.teacherDao().getAllNow(App.profileId) }
                msg?.recipients?.forEach { recipient ->
                    if (recipient.fullName == null) {
                        recipient.fullName = teachers.firstOrNull { it.id == recipient.id }?.fullName ?: ""
                    }
                }

                if (msg?.type == TYPE_SENT && msg.senderName == null) {
                    msg.senderName = app.profile.accountName ?: app.profile.studentNameLong
                }

                msg?.also {
                    //it.recipients = app.db.messageRecipientDao().getAllByMessageId(it.profileId, it.id)
                    if (it.body != null && !it.seen) {
                        app.db.metadataDao().setSeen(it.profileId, it, true)
                    }
                }

            } ?: run {
                activity.navigateUp()
                return@launch
            }

            message = msg
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

        if (app.profile.loginStoreType == LOGIN_TYPE_IDZIENNIK) {
            val meta = "\\[META:([A-z0-9]+);([0-9-]+)]".toRegex().find(message.body!!)
            val messageIdBefore = meta?.get(2)?.toLong() ?: -1

            if (messageIdBefore == -1L) {
                EdziennikTask.messageGet(App.profileId, message).enqueue(activity)
                return
            }
        }


        // if a sent msg is not read by everyone, download it again to check the read status
        if (!checkRecipients() && app.profile.loginStoreType != LoginStore.LOGIN_TYPE_VULCAN) {
            EdziennikTask.messageGet(App.profileId, message).enqueue(activity)
            return
        }

        if(message.type == TYPE_RECEIVED && !message.seen && app.profile.loginStoreType == LoginStore.LOGIN_TYPE_VULCAN) {
            EdziennikTask.messageGet(App.profileId, message).enqueue(activity)
            return
        }

        showMessage()
    }

    private fun checkRecipients(): Boolean {
        message.recipients?.forEach { recipient ->
            if (recipient.id == -1L)
                recipient.fullName = app.profile.accountName ?: app.profile.studentNameLong ?: ""
            if (message.type == TYPE_SENT && recipient.readDate < 1)
                return false
        }
        return true
    }

    private fun showMessage() {
        b.body.text = MessagesUtils.htmlToSpannable(activity, message.body.toString())
        b.date.text = getString(R.string.messages_date_time_format, Date.fromMillis(message.addedDate).formattedStringShort, Time.fromMillis(message.addedDate).stringHM)

        val messageInfo = MessagesUtils.getMessageInfo(app, message, 40, 20, 14, 10)
        b.profileBackground.setImageBitmap(messageInfo.profileImage)
        b.sender.text = messageInfo.profileName

        b.subject.text = message.subject

        b.replyButton.isVisible = message.type == TYPE_RECEIVED || message.type == TYPE_DELETED
        b.deleteButton.isVisible = message.type == TYPE_RECEIVED
        if (message.type == TYPE_RECEIVED || message.type == TYPE_DELETED) {
            activity.navView.apply {
                bottomBar.apply {
                    fabEnable = true
                    fabExtendedText = getString(R.string.messages_reply)
                    fabIcon = CommunityMaterial.Icon2.cmd_reply
                }

                setFabOnClickListener(View.OnClickListener {
                    b.replyButton.performClick()
                })
            }
            activity.gainAttentionFAB()
        }

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
        b.recipients.text = Html.fromHtml(messageRecipients.toString())

        showAttachments()

        BetterLink.attach(b.subject)
        BetterLink.attach(b.body)

        b.progress.visibility = View.GONE
        Anim.fadeIn(b.content, 200, null)
        MessagesFragment.pageSelection = min(message.type, 1)
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
                //if (message.attachmentSizes.isNotNullNorEmpty())
                //    it.putLongArray("attachmentSizes", message.attachmentSizes!!.toLongArray())
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

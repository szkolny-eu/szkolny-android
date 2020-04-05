/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-12.
 */

package pl.szczodrzynski.edziennik.ui.modules.messages

import android.os.Bundle
import android.os.Environment
import android.text.Html
import android.text.TextUtils
import android.view.Gravity.CENTER_VERTICAL
import android.view.Gravity.END
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.google.android.material.chip.Chip
import com.mikepenz.iconics.IconicsColor
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.IconicsSize
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.typeface.library.szkolny.font.SzkolnyFont
import com.mikepenz.iconics.utils.sizeDp
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.events.AttachmentGetEvent
import pl.szczodrzynski.edziennik.data.api.events.AttachmentGetEvent.Companion.TYPE_FINISHED
import pl.szczodrzynski.edziennik.data.api.events.AttachmentGetEvent.Companion.TYPE_PROGRESS
import pl.szczodrzynski.edziennik.data.api.events.MessageGetEvent
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore.Companion.LOGIN_TYPE_IDZIENNIK
import pl.szczodrzynski.edziennik.data.db.entity.Message.Companion.TYPE_RECEIVED
import pl.szczodrzynski.edziennik.data.db.entity.Message.Companion.TYPE_SENT
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.databinding.MessageFragmentBinding
import pl.szczodrzynski.edziennik.utils.Anim
import pl.szczodrzynski.edziennik.utils.BetterLink
import pl.szczodrzynski.edziennik.utils.Themes
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.edziennik.utils.Utils.getStringFromFile
import pl.szczodrzynski.edziennik.utils.Utils.readableFileSize
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.navlib.colorAttr
import java.io.File
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
    private var attachmentList = mutableListOf<Attachment>()

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

        b.replyButton.visibility = if (message.type == TYPE_RECEIVED) View.VISIBLE else View.INVISIBLE
        if (message.type == TYPE_RECEIVED) {
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

    private val attachmentOnClick = { v: View ->
        if (v.tag is Int) {
            downloadAttachment(v.tag as Int)
        }
    }

    private val attachmentOnLongClick = { v: View ->
        (v.tag as? Int)?.let { tag ->
            val popupMenu = PopupMenu(v.context, v)
            popupMenu.menu.add(0, tag, 0, R.string.messages_attachment_download_again)
            popupMenu.setOnMenuItemClickListener {
                downloadAttachment(it.itemId, forceDownload = true)
                true
            }
            popupMenu.show()
        }
        true
    }

    private fun showAttachments() {
        if (message.attachmentIds != null) {
            val insertPoint = b.attachments
            insertPoint.removeAllViews()

            val chipLayoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            chipLayoutParams.setMargins(0, 8.dp, 0, 0)

            val progressLayoutParams = FrameLayout.LayoutParams(18.dp, 18.dp)
            progressLayoutParams.setMargins(8.dp, 0, 8.dp, 0)
            progressLayoutParams.gravity = END or CENTER_VERTICAL

            // CREATE VIEWS AND AN OBJECT FOR EVERY ATTACHMENT

            message.attachmentNames?.forEachIndexed { index, name ->
                val messageId = message.id
                val id = message.attachmentIds?.getOrNull(index) ?: return@forEachIndexed
                val size = message.attachmentSizes?.getOrNull(index) ?: return@forEachIndexed
                // create the parent
                val attachmentLayout = FrameLayout(b.root.context)
                attachmentLayout.setPadding(16.dp, 0, 16.dp, 0)

                val attachmentChip = Chip(attachmentLayout.context)
                //attachmentChip.setChipBackgroundColorResource(ThemeUtils.getChipColorRes());
                attachmentChip.layoutParams = chipLayoutParams
                attachmentChip.chipMinHeight = 40.dp.toFloat()
                //attachmentChip.height = Utils.dpToPx(40)

                // show the file size or not
                if (size == -1L)
                    attachmentChip.text = getString(R.string.messages_attachment_no_size_format, name)
                else
                    attachmentChip.text = getString(R.string.messages_attachment_format, name, readableFileSize(size))
                attachmentChip.ellipsize = TextUtils.TruncateAt.MIDDLE

                // create an icon for the attachment
                val icon: IIcon = when (Utils.getExtensionFromFileName(name)) {
                    "doc", "docx", "odt", "rtf" -> SzkolnyFont.Icon.szf_file_word_outline
                    "xls", "xlsx", "ods" -> SzkolnyFont.Icon.szf_file_excel_outline
                    "ppt", "pptx", "odp" -> SzkolnyFont.Icon.szf_file_powerpoint_outline
                    "pdf" -> SzkolnyFont.Icon.szf_file_pdf_outline
                    "mp3", "wav", "aac" -> SzkolnyFont.Icon.szf_file_music_outline
                    "mp4", "avi", "3gp", "mkv", "flv" -> SzkolnyFont.Icon.szf_file_video_outline
                    "jpg", "jpeg", "png", "bmp", "gif" -> SzkolnyFont.Icon.szf_file_image_outline
                    "zip", "rar", "tar", "7z" -> SzkolnyFont.Icon.szf_zip_box_outline
                    "html", "cpp", "c", "h", "css", "java", "py" -> SzkolnyFont.Icon.szf_file_code_outline
                    else -> CommunityMaterial.Icon.cmd_file_document_outline
                }
                attachmentChip.chipIcon = IconicsDrawable(activity).color(IconicsColor.colorRes(R.color.colorPrimary)).icon(icon).size(IconicsSize.dp(26))
                attachmentChip.closeIcon = IconicsDrawable(activity).icon(CommunityMaterial.Icon.cmd_check).size(IconicsSize.dp(18)).color(IconicsColor.colorInt(Utils.getAttr(activity, android.R.attr.textColorPrimary)))
                attachmentChip.isCloseIconVisible = false
                // set the object's index in the attachmentList as the tag
                attachmentChip.tag = index
                attachmentChip.onClick(attachmentOnClick)
                attachmentChip.onLongClick(attachmentOnLongClick)
                attachmentLayout.addView(attachmentChip)

                val attachmentProgress = ProgressBar(attachmentLayout.context)
                attachmentProgress.layoutParams = progressLayoutParams
                attachmentProgress.visibility = View.GONE
                attachmentLayout.addView(attachmentProgress)

                insertPoint.addView(attachmentLayout)
                // create an object and add to the list
                val a = Attachment(App.profileId, messageId, id, name, size, attachmentLayout, attachmentChip, attachmentProgress)
                attachmentList.add(a)
                // check if the file is already downloaded. Show the check icon if necessary and set `downloaded` to true.
                checkAttachment(a)

            }
        } else {
            // no attachments found
            b.attachmentsTitle.visibility = View.GONE
        }
    }

    private fun downloadAttachment(index: Int, forceDownload: Boolean = false) {
        val attachment = attachmentList[index]

        if (!forceDownload && attachment.downloaded != null) {
            Utils.openFile(activity, File(attachment.downloaded))
            return
        }

        attachment.chip.isEnabled = false
        attachment.chip.setTextColor(Themes.getSecondaryTextColor(activity))
        attachment.progressBar.visibility = View.VISIBLE

        EdziennikTask.attachmentGet(
                App.profileId,
                message,
                attachment.attachmentId,
                attachment.attachmentName
        ).enqueue(activity)
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onAttachmentGetEvent(event: AttachmentGetEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        attachmentList.firstOrNull { it.profileId == event.profileId
                && it.messageId == event.ownerId
                && it.attachmentId == event.attachmentId }?.let { attachment ->

            when (event.eventType) {
                TYPE_FINISHED -> {
                    // save the downloaded file name
                    attachment.downloaded = event.fileName

                    // set the correct name (and size)
                    if (attachment.attachmentSize == -1L)
                        attachment.chip.text = getString(R.string.messages_attachment_no_size_format, attachment.attachmentName)
                    else
                        attachment.chip.text = getString(R.string.messages_attachment_format, attachment.attachmentName, readableFileSize(attachment.attachmentSize))

                    // hide the progress bar and show a tick icon
                    attachment.progressBar.visibility = View.GONE
                    attachment.chip.isEnabled = true
                    attachment.chip.setTextColor(Themes.getPrimaryTextColor(activity))
                    attachment.chip.isCloseIconVisible = true

                    // open the file
                    Utils.openFile(activity, File(attachment.downloaded))
                }

                TYPE_PROGRESS -> {
                    attachment.chip.text = getString(R.string.messages_attachment_downloading_format, attachment.attachmentName, event.bytesWritten.toFloat() / 1000000)
                }
            }
        }
    }

    private fun checkAttachment(attachment: Attachment) {
        val storageDir = Environment.getExternalStoragePublicDirectory("Szkolny.eu")
        storageDir.mkdirs()

        val attachmentDataFile = File(storageDir, "." + attachment.profileId + "_" + attachment.messageId + "_" + attachment.attachmentId)
        if (attachmentDataFile.exists()) {
            try {
                val attachmentFileName = getStringFromFile(attachmentDataFile)
                val attachmentFile = File(attachmentFileName)
                if (attachmentFile.exists()) {
                    attachment.downloaded = attachmentFileName
                    attachment.chip.isCloseIconVisible = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
                //app.apiEdziennik.guiReportException(activity, 355, e)
            }
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

    private class Attachment(
            var profileId: Int,
            var messageId: Long,
            var attachmentId: Long,
            var attachmentName: String,
            var attachmentSize: Long,
            var parent: FrameLayout,
            var chip: Chip,
            var progressBar: ProgressBar
    ) {
        /**
         * An absolute path of the downloaded file. `null` if not downloaded yet.
         */
        internal var downloaded: String? = null
    }
}

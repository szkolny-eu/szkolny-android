/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-4.
 */

package pl.szczodrzynski.edziennik.ui.messages.compose

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.ScrollView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.core.manager.MessageManager.UIConfig
import pl.szczodrzynski.edziennik.core.manager.TextStylingManager.HtmlMode.COMPATIBLE
import pl.szczodrzynski.edziennik.core.manager.TextStylingManager.HtmlMode.ORIGINAL
import pl.szczodrzynski.edziennik.core.manager.TextStylingManager.StylingConfig
import pl.szczodrzynski.edziennik.data.api.ERROR_MESSAGE_NOT_SENT
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.events.MessageSentEvent
import pl.szczodrzynski.edziennik.data.api.events.RecipientListGetEvent
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.enums.LoginType
import pl.szczodrzynski.edziennik.data.enums.NavTarget
import pl.szczodrzynski.edziennik.databinding.MessagesComposeFragmentBinding
import pl.szczodrzynski.edziennik.ext.Bundle
import pl.szczodrzynski.edziennik.ext.DAY
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.ui.dialogs.settings.MessagesConfigDialog
import pl.szczodrzynski.edziennik.ui.messages.list.MessagesFragment
import pl.szczodrzynski.edziennik.utils.DefaultTextStyles
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetSeparatorItem

class MessagesComposeFragment : BaseFragment<MessagesComposeFragmentBinding, MainActivity>(
    inflater = MessagesComposeFragmentBinding::inflate,
) {
    companion object {
        private const val TAG = "MessagesComposeFragment"
    }

    override fun getFab() = R.string.messages_compose_send to CommunityMaterial.Icon3.cmd_send_outline
    override fun getBottomSheetItems() = listOf(
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.messages_compose_send_long)
            .withIcon(CommunityMaterial.Icon3.cmd_send_outline)
            .withOnClickListener {
                activity.bottomSheet.close()
                sendMessage()
            },
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.messages_compose_save_draft)
            .withIcon(CommunityMaterial.Icon.cmd_content_save_edit_outline)
            .withOnClickListener {
                activity.bottomSheet.close()
                saveDraft()
            },
        BottomSheetSeparatorItem(true),
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
    private val textStylingManager
        get() = app.textStylingManager
    private val greetingText
        get() = app.profile.config.ui.messagesGreetingText ?: "\n\nZ poważaniem\n${app.profile.accountOwnerName}"

    private val teachers = mutableListOf<Teacher>()

    private lateinit var stylingConfig: StylingConfig
    private lateinit var uiConfig: UIConfig
    private var changedRecipients = false
    private var changedSubject = false
    private var changedBody = false
    private var discardDraftItem: BottomSheetPrimaryItem? = null
    private var draftMessageId: Long? = null

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        b.breakpoints.visibility = if (App.devMode) View.VISIBLE else View.GONE
        b.breakpoints.setOnClickListener {
            b.breakpoints.isEnabled = true
            @SuppressLint("SetTextI18n")
            b.breakpoints.text = "Breakpoints!"
            // do your job
        }

        discardDraftItem = BottomSheetPrimaryItem(true)
            .withTitle(R.string.messages_compose_discard_draft)
            .withIcon(CommunityMaterial.Icon3.cmd_text_box_remove_outline)
            .withOnClickListener {
                activity.bottomSheet.close()
                discardDraftDialog()
            }

        getRecipientList()
        createView()
    }

    private fun getMessageBody(): String {
        return if (app.data.messagesConfig.textStyling)
            textStylingManager.getHtmlText(stylingConfig)
        else
            b.text.text?.toString() ?: ""
    }

    private suspend fun getRecipientList() {
        if (app.data.messagesConfig.syncRecipientList && System.currentTimeMillis() - app.profile.lastReceiversSync > 1 * DAY * 1000) {
            activity.snackbar("Pobieranie listy odbiorców...")
            EdziennikTask.recipientListGet(App.profileId).enqueue(activity)
        }
        else {
            val list = withContext(Dispatchers.IO) {
                app.db.teacherDao().getAllNow(App.profileId).filter { it.loginId != null }
            }
            updateRecipientList(list)
        }
    }

    private fun createView() {
        b.recipientsLayout.setBoxCornerRadii(0f, 0f, 0f, 0f)
        b.subjectLayout.setBoxCornerRadii(0f, 0f, 0f, 0f)
        b.textLayout.setBoxCornerRadii(0f, 0f, 0f, 0f)

        b.recipients.addTextChangedListener(onTextChanged = { _, _, _, _ ->
            b.recipientsLayout.error = null
            changedRecipients = true
        })
        b.subject.addTextChangedListener(onTextChanged = { _, _, _, _ ->
            b.subjectLayout.error = null
            changedSubject = true
        })
        b.text.addTextChangedListener(onTextChanged = { _, _, _, _ ->
            b.textLayout.error = null
            changedBody = true
        })

        b.subjectLayout.counterMaxLength = when (app.profile.loginStoreType) {
            LoginType.MOBIDZIENNIK -> 100
            LoginType.LIBRUS -> 150
            LoginType.VULCAN -> 200
            LoginType.IDZIENNIK -> 180
            LoginType.EDUDZIENNIK -> 0
            else -> -1
        }
        b.textLayout.counterMaxLength = when (app.profile.loginStoreType) {
            LoginType.MOBIDZIENNIK -> -1
            LoginType.LIBRUS -> 20000
            LoginType.VULCAN -> -1
            LoginType.IDZIENNIK -> 1983
            LoginType.EDUDZIENNIK -> 0
            else -> -1
        }

        b.recipients.chipTokenizer = MessagesComposeChipTokenizer(activity, b.recipients, teachers)
        b.recipients.setIllegalCharacterIdentifier { c ->
            c.toString().matches("[\\n;:_ ]".toRegex())
        }
        b.recipients.setOnChipRemoveListener {
            b.recipients.setSelection(b.recipients.text.length)
        }

        b.recipients.addTextChangedListener( beforeTextChanged = { _, _, _, _ ->
            b.recipients.ignoreThreshold = false
        })
        b.recipients.onDismissListener = AutoCompleteTextView.OnDismissListener {
            b.recipients.ignoreThreshold = false
        }
        b.recipientsLayout.setEndIconOnClickListener {
            b.recipients.error = null
            b.recipients.ignoreThreshold = true
            b.recipients.showDropDown()
            val adapter = b.recipients.adapter ?: return@setEndIconOnClickListener
            if (adapter is MessagesComposeSuggestionAdapter)
                adapter.filter.filter(null)
        }

        b.recipientsLayout.isEnabled = false
        b.subjectLayout.isEnabled = false
        b.textLayout.isEnabled = false

        val styles = DefaultTextStyles.getAsList(b.fontStyle)

        uiConfig = UIConfig(
            context = activity,
            recipients = b.recipients,
            subject = b.subject,
            body = b.text,
            teachers = teachers,
            greetingOnCompose = app.profile.config.ui.messagesGreetingOnCompose,
            greetingOnReply = app.profile.config.ui.messagesGreetingOnReply,
            greetingOnForward = app.profile.config.ui.messagesGreetingOnForward,
            greetingText = greetingText,
        )
        stylingConfig = StylingConfig(
            editText = b.text,
            fontStyleGroup = b.fontStyle.styles,
            fontStyleClear = b.fontStyle.clear,
            styles = styles,
            textHtml = if (App.devMode) b.textHtml else null,
            htmlMode = when (app.profile.loginStoreType) {
                LoginType.MOBIDZIENNIK -> COMPATIBLE
                else -> ORIGINAL
            },
        )

        b.fontStyle.root.isVisible = app.data.messagesConfig.textStyling
        if (app.data.messagesConfig.textStyling) {
            textStylingManager.attach(stylingConfig)
            b.fontStyle.styles.addOnButtonCheckedListener { _, _, _ ->
                changedBody = true
            }
        }
    }

    override suspend fun onFabClick() {
        sendMessage()
    }

    private fun onBeforeNavigate(): Boolean {
        val messageText = b.text.text?.toString()?.trim() ?: ""
        val greetingText = this.greetingText.trim()
        // navigateUp if nothing changed
        if ((!changedRecipients || b.recipients.allChips.isEmpty())
            && (!changedSubject || b.subject.text.isNullOrBlank())
            && (!changedBody || messageText.isEmpty() || messageText == greetingText)
        )
            return true
        saveDraftDialog()
        return false
    }

    private fun saveDraftDialog() {
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.messages_compose_save_draft_title)
            .setMessage(R.string.messages_compose_save_draft_text)
            .setPositiveButton(R.string.save) { _, _ ->
                saveDraft()
                MessagesFragment.pageSelection = Message.TYPE_DRAFT
                activity.navigate(navTarget = NavTarget.MESSAGES, skipBeforeNavigate = true)
            }
            .setNegativeButton(R.string.discard) { _, _ ->
                activity.resumePausedNavigation()
            }
            .show()
    }

    private fun saveDraft() {
        launch {
            manager.saveAsDraft(uiConfig, stylingConfig, App.profileId, draftMessageId)
            Toast.makeText(activity, R.string.messages_compose_draft_saved, Toast.LENGTH_SHORT).show()
            changedRecipients = false
            changedSubject = false
            changedBody = false
        }
        if (discardDraftItem != null)
            activity.bottomSheet.addItemAt(2, discardDraftItem!!)
        discardDraftItem = null
    }

    private fun discardDraftDialog() {
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.messages_compose_discard_draft_title)
            .setMessage(R.string.messages_compose_discard_draft_text)
            .setPositiveButton(R.string.remove) { _, _ ->
                launch {
                    if (draftMessageId != null)
                        manager.deleteDraft(App.profileId, draftMessageId!!)
                    Toast.makeText(activity, R.string.messages_compose_draft_discarded, Toast.LENGTH_SHORT).show()
                    activity.navigateUp(skipBeforeNavigate = true)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    @SuppressLint("SetTextI18n")
    private fun updateRecipientList(list: List<Teacher>) { launch {
        withContext(Dispatchers.Default) {
            teachers.clear()
            teachers.addAll(list.sortedBy { it.fullName })
            Teacher.types.mapTo(teachers) {
                Teacher(-1, -it.toLong(), Teacher.typeName(activity, it), "")
            }
            /*teachers.forEach {
                println(it)
            }*/
        }

        b.recipientsLayout.isEnabled = true
        b.subjectLayout.isEnabled = true
        b.textLayout.isEnabled = true

        val adapter = MessagesComposeSuggestionAdapter(activity, teachers)
        b.recipients.setAdapter(adapter)

        val message = manager.fillWithBundle(uiConfig, arguments)
        if (message != null && message.isDraft) {
            draftMessageId = message.id
            if (discardDraftItem != null)
                activity.bottomSheet.addItemAt(2, discardDraftItem!!)
            discardDraftItem = null
        }

        when {
            b.recipients.text.isBlank() -> b.recipients.requestFocus()
            b.subject.text.isNullOrBlank() -> b.subject.requestFocus()
            else -> b.text.requestFocus()
        }

        if (!app.data.messagesConfig.textStyling)
            b.text.setText(b.text.text?.toString())
        b.text.setSelection(0)
         (b.root as? ScrollView)?.smoothScrollTo(0, 0)

        changedRecipients = false
        changedSubject = false
        changedBody = false
    }}

    private fun sendMessage() {
        b.recipientsLayout.error = null
        b.subjectLayout.error = null
        b.textLayout.error = null

        if (b.recipients.tokenValues.isNotEmpty()) {
            b.recipientsLayout.error = getString(R.string.messages_compose_recipients_error)
            return
        }
        val recipients = mutableSetOf<Teacher>()
        b.recipients.allChips.forEach { chip ->
            if (chip.data !is Teacher)
                return@forEach
            val teacher = chip.data as Teacher

            recipients += teacher
            //println(teacher)
        }
        val subject = b.subject.text?.toString()
        val text = b.text.text
        if (recipients.isEmpty()) {
            b.recipientsLayout.error = getString(R.string.messages_compose_recipients_empty)
            return
        }
        if (subject.isNullOrBlank() || subject.length < 3) {
            b.subjectLayout.error = getString(R.string.messages_compose_subject_empty)
            return
        }
        if (text.isNullOrBlank() || text.length < 3) {
            b.textLayout.error = getString(R.string.messages_compose_text_empty)
            return
        }

        // do magic
        // apparently this removes an underline
        // span from the text where the caret is
        b.subject.requestFocus()
        b.subject.clearFocus()
        activity.navView.bottomSheet.hideKeyboard()
        b.text.clearFocus()
        b.text.setSelection(0)

        if (b.subjectLayout.counterMaxLength != -1 && b.subject.length() > b.subjectLayout.counterMaxLength)
            return
        if (b.textLayout.counterMaxLength != -1 && b.text.length() > b.textLayout.counterMaxLength)
            return

        val body = getMessageBody()

        activity.bottomSheet.hideKeyboard()

        MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.messages_compose_confirm_title)
                .setMessage(R.string.messages_compose_confirm_text)
                .setPositiveButton(R.string.send) { _, _ ->
                    EdziennikTask.messageSend(App.profileId, recipients, subject.trim(), body).enqueue(activity)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    override fun onResume() {
        super.onResume()
        if (!isAdded)
            return
        activity.onBeforeNavigate = this::onBeforeNavigate
    }

    override fun onPause() {
        super.onPause()
        activity.onBeforeNavigate = null
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onRecipientListGetEvent(event: RecipientListGetEvent) {
        if (event.profileId != App.profileId)
            return
        EventBus.getDefault().removeStickyEvent(event)

        activity.snackbarDismiss()
        updateRecipientList(event.teacherList)
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onMessageSentEvent(event: MessageSentEvent) {
        if (event.profileId != App.profileId)
            return
        EventBus.getDefault().removeStickyEvent(event)

        if (event.message == null) {
            activity.error(ApiError(TAG, ERROR_MESSAGE_NOT_SENT))
            return
        }

        if (draftMessageId != null) {
            launch {
                manager.deleteDraft(App.profileId, draftMessageId!!)
            }
        }

        activity.snackbar(app.getString(R.string.messages_sent_success), app.getString(R.string.ok))
        activity.navigate(navTarget = NavTarget.MESSAGE, args = Bundle(
                "messageId" to event.message.id,
                "message" to app.gson.toJson(event.message),
                "sentDate" to event.sentDate
        ), skipBeforeNavigate = true)
    }
}

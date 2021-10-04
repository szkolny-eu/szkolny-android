/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-4.
 */

package pl.szczodrzynski.edziennik.ui.modules.messages.compose

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.*
import android.text.Spanned.*
import android.text.style.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hootsuite.nachos.chip.ChipInfo
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.ERROR_MESSAGE_NOT_SENT
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.events.MessageSentEvent
import pl.szczodrzynski.edziennik.data.api.events.RecipientListGetEvent
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.db.entity.LoginStore
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.databinding.MessagesComposeFragmentBinding
import pl.szczodrzynski.edziennik.ui.dialogs.MessagesConfigDialog
import pl.szczodrzynski.edziennik.ui.modules.messages.MessagesUtils
import pl.szczodrzynski.edziennik.ui.modules.messages.MessagesUtils.getProfileImage
import pl.szczodrzynski.edziennik.utils.Themes
import pl.szczodrzynski.edziennik.utils.html.BetterHtml
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.edziennik.utils.span.BoldSpan
import pl.szczodrzynski.edziennik.utils.span.ItalicSpan
import pl.szczodrzynski.edziennik.utils.span.SubscriptSizeSpan
import pl.szczodrzynski.edziennik.utils.span.SuperscriptSizeSpan
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem
import kotlin.coroutines.CoroutineContext
import kotlin.text.replace

class MessagesComposeFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "MessagesComposeFragment"
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: MessagesComposeFragmentBinding

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private val profileConfig by lazy { app.config.forProfile().ui }
    private val greetingText
        get() = profileConfig.messagesGreetingText ?: "\n\nZ poważaniem\n${app.profile.accountOwnerName}"

    private var teachers = mutableListOf<Teacher>()

    private val userAddedSpans = listOf(
        BoldSpan::class.java,
        ItalicSpan::class.java,
        UnderlineSpan::class.java,
        StrikethroughSpan::class.java,
        SubscriptSizeSpan::class.java,
        SuperscriptSizeSpan::class.java,
    )
    private var watchFormatChecked = true
    private var watchSelectionChanged = true

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        requireContext().theme.applyStyle(Themes.appTheme, true)
        // activity, context and profile is valid
        b = MessagesComposeFragmentBinding.inflate(inflater)
        return b.root
    }
    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // TODO check if app, activity, b can be null
        if (!isAdded)
            return

        EventBus.getDefault().register(this)

        b.breakpoints.visibility = if (App.devMode) View.VISIBLE else View.GONE
        b.breakpoints.setOnClickListener {
            b.breakpoints.isEnabled = true
            b.breakpoints.text = "Breakpoints!"
            // do your job
        }

        if (App.devMode) {
            b.textHtml.isVisible = true
            b.text.addTextChangedListener {
                b.textHtml.text = getHtmlText()
            }
        }

        activity.bottomSheet.prependItem(
            BottomSheetPrimaryItem(true)
                .withTitle(R.string.menu_messages_config)
                .withIcon(CommunityMaterial.Icon.cmd_cog_outline)
                .withOnClickListener {
                    activity.bottomSheet.close()
                    MessagesConfigDialog(activity, false, null, null)
                }
        )

        launch {
            delay(100)
            getRecipientList()

            createView()
        }
    }

    private fun getHtmlText(): String {
        val text = b.text.text ?: return ""

        // apparently setting the spans to a different Spannable calls the original EditText's
        // onSelectionChanged with selectionStart=-1, which in effect unchecks the format toggles
        watchSelectionChanged = false
        var textHtml = if (app.profile.loginStoreType != LoginStore.LOGIN_TYPE_VULCAN) {
            val spanned = SpannableString(text)
            // remove zero-length spans, as they seem to affect
            // the whole line when converted to HTML
            spanned.getSpans(0, spanned.length, Any::class.java).forEach {
                val spanStart = spanned.getSpanStart(it)
                val spanEnd = spanned.getSpanEnd(it)
                if (spanStart == spanEnd && it::class.java in userAddedSpans)
                    spanned.removeSpan(it)
            }
            HtmlCompat.toHtml(spanned, HtmlCompat.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL)
                .replace("\n", "")
                .replace(" dir=\"ltr\"", "")
                .replace("</b><b>", "")
                .replace("</i><i>", "")
                .replace("</u><u>", "")
                .replace("</sub><sub>", "")
                .replace("</sup><sup>", "")
                .replace("p style=\"margin-top:0; margin-bottom:0;\"", "p")
        } else {
            text.toString()
        }
        watchSelectionChanged = true

        if (app.profile.loginStoreType == LoginStore.LOGIN_TYPE_MOBIDZIENNIK) {
            textHtml = textHtml
                .replace("<br>", "<p>&nbsp;</p>")
                .replace("<b>", "<strong>")
                .replace("</b>", "</strong>")
                .replace("<i>", "<em>")
                .replace("</i>", "</em>")
                .replace("<u>", "<span style=\"text-decoration: underline;\">")
                .replace("</u>", "</span>")
        }

        return textHtml
    }

    private fun getRecipientList() {
        if (System.currentTimeMillis() - app.profile.lastReceiversSync > 1 * DAY * 1000 && app.profile.loginStoreType != LoginStore.LOGIN_TYPE_VULCAN) {
            activity.snackbar("Pobieranie listy odbiorców...")
            EdziennikTask.recipientListGet(App.profileId).enqueue(activity)
        }
        else {
            launch {
                val list = withContext(Dispatchers.Default) {
                    app.db.teacherDao().getAllNow(App.profileId).filter { it.loginId != null }
                }
                updateRecipientList(list)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onFormatChecked(
        group: MaterialButtonToggleGroup,
        checkedId: Int,
        isChecked: Boolean
    ) {
        if (!watchFormatChecked)
            return
        val span = when (checkedId) {
            R.id.fontStyleBold -> BoldSpan()
            R.id.fontStyleItalic -> ItalicSpan()
            R.id.fontStyleUnderline -> UnderlineSpan()
            R.id.fontStyleStrike -> StrikethroughSpan()
            R.id.fontStyleSubscript -> SubscriptSizeSpan(10, dip = true)
            R.id.fontStyleSuperscript -> SuperscriptSizeSpan(10, dip = true)
            else -> return
        }

        // see comments in getHtmlText()
        watchSelectionChanged = false
        if (isChecked)
            BetterHtml.applyFormat(span, b.text)
        else
            BetterHtml.removeFormat(span, b.text)
        watchSelectionChanged = true

        if (App.devMode)
            b.textHtml.text = getHtmlText()
    }

    private fun onSelectionChanged(selectionStart: Int, selectionEnd: Int) {
        if (!watchSelectionChanged)
            return
        val spanned = b.text.text ?: return
        val spans = spanned.getSpans(selectionStart, selectionEnd, Any::class.java).mapNotNull {
            if (it::class.java !in userAddedSpans)
                return@mapNotNull null
            val spanStart = spanned.getSpanStart(it)
            val spanEnd = spanned.getSpanEnd(it)
            // remove 0-length spans after navigating out of them
            if (spanStart == spanEnd)
                spanned.removeSpan(it)
            else if (spanned.getSpanFlags(it) hasSet SPAN_EXCLUSIVE_EXCLUSIVE)
                spanned.setSpan(it, spanStart, spanEnd, SPAN_EXCLUSIVE_INCLUSIVE)

            // names are helpful here
            val isNotAfterWord = selectionEnd <= spanEnd
            val isSelectionInWord = selectionStart != selectionEnd && selectionStart >= spanStart
            val isCursorInWord = selectionStart == selectionEnd && selectionStart > spanStart
            val isChecked = (isCursorInWord || isSelectionInWord) && isNotAfterWord
            if (isChecked) it::class.java else null
        }
        watchFormatChecked = false
        b.fontStyleBold.isChecked = BoldSpan::class.java in spans
        b.fontStyleItalic.isChecked = ItalicSpan::class.java in spans
        b.fontStyleUnderline.isChecked = UnderlineSpan::class.java in spans
        b.fontStyleStrike.isChecked = StrikethroughSpan::class.java in spans
        b.fontStyleSubscript.isChecked = SubscriptSizeSpan::class.java in spans
        b.fontStyleSuperscript.isChecked = SuperscriptSizeSpan::class.java in spans
        watchFormatChecked = true
    }

    private fun createView() {
        b.recipientsLayout.setBoxCornerRadii(0f, 0f, 0f, 0f)
        b.subjectLayout.setBoxCornerRadii(0f, 0f, 0f, 0f)
        b.textLayout.setBoxCornerRadii(0f, 0f, 0f, 0f)

        b.recipients.addTextChangedListener(onTextChanged = { _, _, _, _ ->
            b.recipientsLayout.error = null
        })
        b.subject.addTextChangedListener(onTextChanged = { _, _, _, _ ->
            b.subjectLayout.error = null
        })
        b.text.addTextChangedListener(onTextChanged = { _, _, _, _ ->
            b.textLayout.error = null
        })

        b.subjectLayout.counterMaxLength = when (app.profile.loginStoreType) {
            LoginStore.LOGIN_TYPE_MOBIDZIENNIK -> 100
            LoginStore.LOGIN_TYPE_LIBRUS -> 150
            LoginStore.LOGIN_TYPE_VULCAN -> 200
            LoginStore.LOGIN_TYPE_IDZIENNIK -> 180
            LoginStore.LOGIN_TYPE_EDUDZIENNIK -> 0
            else -> -1
        }
        b.textLayout.counterMaxLength = when (app.profile.loginStoreType) {
            LoginStore.LOGIN_TYPE_MOBIDZIENNIK -> -1
            LoginStore.LOGIN_TYPE_LIBRUS -> 20000
            LoginStore.LOGIN_TYPE_VULCAN -> -1
            LoginStore.LOGIN_TYPE_IDZIENNIK -> 1983
            LoginStore.LOGIN_TYPE_EDUDZIENNIK -> 0
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

        b.fontStyleBold.text = CommunityMaterial.Icon2.cmd_format_bold.character.toString()
        b.fontStyleItalic.text = CommunityMaterial.Icon2.cmd_format_italic.character.toString()
        b.fontStyleUnderline.text = CommunityMaterial.Icon2.cmd_format_underline.character.toString()
        b.fontStyleStrike.text = CommunityMaterial.Icon2.cmd_format_strikethrough.character.toString()
        b.fontStyleSubscript.text = CommunityMaterial.Icon2.cmd_format_subscript.character.toString()
        b.fontStyleSuperscript.text = CommunityMaterial.Icon2.cmd_format_superscript.character.toString()

        b.fontStyle.addOnButtonCheckedListener(this::onFormatChecked)
        b.text.setSelectionChangedListener(this::onSelectionChanged)

        activity.navView.bottomBar.apply {
            fabEnable = true
            fabExtendedText = getString(R.string.messages_compose_send)
            fabIcon = CommunityMaterial.Icon3.cmd_send_outline

            setFabOnClickListener {
                sendMessage()
            }
        }

        activity.gainAttentionFAB()
    }

    @SuppressLint("SetTextI18n")
    private fun updateRecipientList(list: List<Teacher>) { launch {
        withContext(Dispatchers.Default) {
            teachers = list.sortedBy { it.fullName }.toMutableList()
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

        if (profileConfig.messagesGreetingOnCompose)
            b.text.setText(greetingText)

        handleReplyMessage()
        handleMailToIntent()
    }}

    private fun handleReplyMessage() = launch {
        val replyMessage = arguments?.getString("message")
        if (replyMessage != null) {
            val chipList = mutableListOf<ChipInfo>()
            var subject = ""
            val span = SpannableStringBuilder()
            var body: CharSequence = ""

            withContext(Dispatchers.Default) {
                val msg = app.gson.fromJson(replyMessage, MessageFull::class.java)
                val dateString = getString(R.string.messages_date_time_format, Date.fromMillis(msg.addedDate).formattedStringShort, Time.fromMillis(msg.addedDate).stringHM)
                // add original message info
                span.appendText("W dniu ")
                span.appendSpan(dateString, ItalicSpan(), SPAN_EXCLUSIVE_EXCLUSIVE)
                span.appendText(", ")
                span.appendSpan(msg.senderName.fixName(), ItalicSpan(), SPAN_EXCLUSIVE_EXCLUSIVE)
                span.appendText(" napisał(a):")
                span.setSpan(BoldSpan(), 0, span.length, SPAN_EXCLUSIVE_EXCLUSIVE)
                span.appendText("\n\n")

                if (arguments?.getString("type") == "reply") {
                    // add greeting text
                    if (profileConfig.messagesGreetingOnReply)
                        span.replace(0, 0, "$greetingText\n\n\n")
                    else
                        span.replace(0, 0, "\n\n")

                    teachers.firstOrNull { it.id == msg.senderId }?.let { teacher ->
                        teacher.image = getProfileImage(48, 24, 16, 12, 1, teacher.fullName)
                        chipList += ChipInfo(teacher.fullName, teacher)
                    }
                    subject = "Re: ${msg.subject}"
                } else {
                    // add greeting text
                    if (profileConfig.messagesGreetingOnForward)
                        span.replace(0, 0, "$greetingText\n\n\n")
                    else
                        span.replace(0, 0, "\n\n")

                    subject = "Fwd: ${msg.subject}"
                }
                body = MessagesUtils.htmlToSpannable(activity, msg.body
                        ?: "Nie udało się wczytać oryginalnej wiadomości.")//Html.fromHtml(msg.body?.replace("<br\\s?/?>".toRegex(), "\n") ?: "Nie udało się wczytać oryginalnej wiadomości.")
            }

            b.recipients.addTextWithChips(chipList)
            if (b.recipients.text.isNullOrEmpty())
                b.recipients.requestFocus()
            else
                b.text.requestFocus()
            b.subject.setText(subject)
            b.text.apply {
                text = span.appendText(body)
                setSelection(0)
            }
            b.root.scrollTo(0, 0)
        }
        else {
            b.recipients.requestFocus()
        }
    }

    private fun handleMailToIntent() {
        val teacherId = arguments?.getLong("messageRecipientId")
        if (teacherId == 0L)
            return

        val chipList = mutableListOf<ChipInfo>()
        teachers.firstOrNull { it.id == teacherId }?.let { teacher ->
            teacher.image = getProfileImage(48, 24, 16, 12, 1, teacher.fullName)
            chipList += ChipInfo(teacher.fullName, teacher)
        }
        b.recipients.addTextWithChips(chipList)

        val subject = arguments?.getString("messageSubject")
        b.subject.setText(subject ?: return)
    }

    private fun sendMessage() {
        b.recipientsLayout.error = null
        b.subjectLayout.error = null
        b.textLayout.error = null

        if (b.recipients.tokenValues.isNotEmpty()) {
            b.recipientsLayout.error = getString(R.string.messages_compose_recipients_error)
            return
        }
        val recipients = mutableListOf<Teacher>()
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

        val textHtml = getHtmlText()

        activity.bottomSheet.hideKeyboard()

        MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.messages_compose_confirm_title)
                .setMessage(R.string.messages_compose_confirm_text)
                .setPositiveButton(R.string.send) { _, _ ->
                    EdziennikTask.messageSend(App.profileId, recipients, subject.trim(), textHtml).enqueue(activity)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
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

        activity.snackbar(app.getString(R.string.messages_sent_success), app.getString(R.string.ok))
        activity.loadTarget(MainActivity.TARGET_MESSAGES_DETAILS, Bundle(
                "messageId" to event.message.id,
                "message" to app.gson.toJson(event.message),
                "sentDate" to event.sentDate
        ))
    }
}

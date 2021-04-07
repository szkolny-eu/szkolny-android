/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-4.
 */

package pl.szczodrzynski.edziennik.ui.modules.messages.compose

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.text.HtmlCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.hootsuite.nachos.ChipConfiguration
import com.hootsuite.nachos.chip.ChipInfo
import com.hootsuite.nachos.chip.ChipSpan
import com.hootsuite.nachos.chip.ChipSpanChipCreator
import com.hootsuite.nachos.tokenizer.SpanChipTokenizer
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
import pl.szczodrzynski.edziennik.ui.modules.messages.MessagesUtils
import pl.szczodrzynski.edziennik.ui.modules.messages.MessagesUtils.getProfileImage
import pl.szczodrzynski.edziennik.utils.Colors
import pl.szczodrzynski.edziennik.utils.Themes
import pl.szczodrzynski.edziennik.utils.Themes.getPrimaryTextColor
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.navlib.elevateSurface
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

    private var teachers = mutableListOf<Teacher>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        context!!.theme.applyStyle(Themes.appTheme, true)
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

        /*b.fontStyleBold.icon = IconicsDrawable(activity, CommunityMaterial.Icon.cmd_format_bold)
                .sizeDp(24)
                .colorAttr(activity, R.attr.colorOnPrimary)
        b.fontStyleItalic.icon = IconicsDrawable(activity, CommunityMaterial.Icon.cmd_format_italic)
                .sizeDp(24)
                .colorAttr(activity, R.attr.colorOnPrimary)
        b.fontStyleUnderline.icon = IconicsDrawable(activity, CommunityMaterial.Icon.cmd_format_underline)
                .sizeDp(24)
                .colorAttr(activity, R.attr.colorOnPrimary)

        b.fontStyle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            val span: Any = when (checkedId) {
                R.id.fontStyleBold -> StyleSpan(Typeface.BOLD)
                R.id.fontStyleItalic -> StyleSpan(Typeface.ITALIC)
                R.id.fontStyleUnderline -> UnderlineSpan()
                else -> StyleSpan(Typeface.NORMAL)
            }

            if (isChecked) {
                val flags = if (b.text.selectionStart == b.text.selectionEnd)
                    SpannableString.SPAN_INCLUSIVE_INCLUSIVE
                else
                    SpannableString.SPAN_EXCLUSIVE_INCLUSIVE
                b.text.text?.setSpan(span, b.text.selectionStart, b.text.selectionEnd, flags)
            }
            else {
                b.text.text?.getSpans(b.text.selectionStart, b.text.selectionEnd, span.javaClass)?.forEach {
                    if (it is StyleSpan && span is StyleSpan && it.style == span.style)
                        b.text.text?.removeSpan(it)
                    else if (it.javaClass == span.javaClass)
                        b.text.text?.removeSpan(it)
                }
            }
        }*/

        launch {
            delay(100)
            getRecipientList()

            createView()
        }
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

        b.recipients.chipTokenizer = SpanChipTokenizer<ChipSpan>(activity, object : ChipSpanChipCreator() {
            override fun createChip(context: Context, text: CharSequence, data: Any?): ChipSpan? {
                if (data == null || data !is Teacher)
                    return null
                if (data.id !in -24L..0L) {
                    b.recipients.allChips.forEach {
                        if (it.data == data) {
                            Toast.makeText(activity, R.string.messages_compose_recipient_exists, Toast.LENGTH_SHORT).show()
                            return null
                        }
                    }
                    val chipSpan = ChipSpan(context, data.fullName, BitmapDrawable(context.resources, data.image), data)
                    chipSpan.setIconBackgroundColor(Colors.stringToMaterialColor(data.fullName))
                    return chipSpan
                }

                val type = (data.id * -1).toInt()

                val textColorPrimary = android.R.attr.textColorPrimary.resolveAttr(activity)
                val textColorSecondary = android.R.attr.textColorSecondary.resolveAttr(activity)

                val sortByCategory = type in listOf(
                        Teacher.TYPE_PARENTS_COUNCIL,
                        Teacher.TYPE_EDUCATOR,
                        Teacher.TYPE_STUDENT
                )
                val teachers = if (sortByCategory)
                    teachers.sortedBy { it.typeDescription }
                else
                    teachers

                val category = mutableListOf<Teacher>()
                val categoryNames = mutableListOf<CharSequence>()
                val categoryCheckedItems = mutableListOf<Boolean>()
                teachers.forEach { teacher ->
                    if (!teacher.isType(type))
                        return@forEach

                    category += teacher
                    val name = teacher.fullName
                    val description = when (type) {
                        Teacher.TYPE_TEACHER -> null
                        Teacher.TYPE_PARENTS_COUNCIL -> teacher.typeDescription
                        Teacher.TYPE_SCHOOL_PARENTS_COUNCIL -> null
                        Teacher.TYPE_PEDAGOGUE -> null
                        Teacher.TYPE_LIBRARIAN -> null
                        Teacher.TYPE_SCHOOL_ADMIN -> null
                        Teacher.TYPE_SUPER_ADMIN -> null
                        Teacher.TYPE_SECRETARIAT -> null
                        Teacher.TYPE_PRINCIPAL -> null
                        Teacher.TYPE_EDUCATOR -> teacher.typeDescription
                        Teacher.TYPE_PARENT -> teacher.typeDescription
                        Teacher.TYPE_STUDENT -> teacher.typeDescription
                        Teacher.TYPE_SPECIALIST -> null
                        else -> teacher.typeDescription
                    }
                    categoryNames += listOfNotNull(
                            name.asSpannable(
                                    ForegroundColorSpan(textColorPrimary)
                            ),
                            description?.asSpannable(
                                    ForegroundColorSpan(textColorSecondary),
                                    AbsoluteSizeSpan(14.dp)
                            )
                    ).concat("\n")

                    // check the teacher if already added as a recipient
                    categoryCheckedItems += b.recipients.allChips.firstOrNull { it.data == teacher } != null
                }

                MaterialAlertDialogBuilder(activity)
                        .setTitle("Dodaj odbiorców - "+ Teacher.typeName(activity, type))
                        //.setMessage(getString(R.string.messages_compose_recipients_text_format, Teacher.typeName(activity, type)))
                        .setPositiveButton("OK", null)
                        .setNeutralButton("Anuluj", null)
                        .setMultiChoiceItems(categoryNames.toTypedArray(), categoryCheckedItems.toBooleanArray()) { _, which, isChecked ->
                            val teacher = category[which]
                            if (isChecked) {
                                val chipInfoList = mutableListOf<ChipInfo>()
                                teacher.image = getProfileImage(48, 24, 16, 12, 1, teacher.fullName)
                                chipInfoList.add(ChipInfo(teacher.fullName, teacher))
                                b.recipients.addTextWithChips(chipInfoList)
                            }
                            else {
                                b.recipients.allChips.forEach {
                                    if (it.data == teacher)
                                        b.recipients.chipTokenizer?.deleteChipAndPadding(it, b.recipients.text)
                                }
                            }
                        }
                        .show()
                return null
            }

            override fun configureChip(chip: ChipSpan, chipConfiguration: ChipConfiguration) {
                super.configureChip(chip, chipConfiguration)
                chip.setBackgroundColor(elevateSurface(activity, 8).toColorStateList())
                chip.setTextColor(getPrimaryTextColor(activity))
            }
        }, ChipSpan::class.java)

        b.recipients.setIllegalCharacterIdentifier { c ->
            c.toString().matches("[\\n;:_ ]".toRegex())
        }
        b.recipients.setOnChipRemoveListener { _ ->
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

        activity.navView.bottomBar.apply {
            fabEnable = true
            fabExtendedText = getString(R.string.messages_compose_send)
            fabIcon = CommunityMaterial.Icon3.cmd_send_outline

            setFabOnClickListener(View.OnClickListener {
                sendMessage()
            })
        }

        activity.gainAttentionFAB()
    }

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

        handleReplyMessage()
    }}

    private fun handleReplyMessage() { launch {
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
                span.appendSpan(dateString, StyleSpan(Typeface.ITALIC), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                span.appendText(", ")
                span.appendSpan(msg.senderName.fixName(), StyleSpan(Typeface.ITALIC), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                span.appendText(" napisał(a):")
                span.setSpan(StyleSpan(Typeface.BOLD), 0, span.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                span.appendText("\n\n")

                if (arguments?.getString("type") == "reply") {
                    // add greeting text
                    span.replace(0, 0, "\n\nZ poważaniem,\n${app.profile.accountName
                            ?: app.profile.studentNameLong ?: ""}\n\n\n")

                    teachers.firstOrNull { it.id == msg.senderId }?.let { teacher ->
                        teacher.image = getProfileImage(48, 24, 16, 12, 1, teacher.fullName)
                        chipList += ChipInfo(teacher.fullName, teacher)
                    }
                    subject = "Re: ${msg.subject}"
                } else {
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
    }}

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

        var textHtml = if (app.profile.loginStoreType != LoginStore.LOGIN_TYPE_VULCAN) {
            HtmlCompat.toHtml(SpannableString(text), HtmlCompat.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL)
                    .replace("\n", "")
                    .replace(" dir=\"ltr\"", "")
        }
        else {
            text.toString()
        }

        if (app.profile.loginStoreType == LoginStore.LOGIN_TYPE_MOBIDZIENNIK) {
            textHtml = textHtml
                    .replace("p style=\"margin-top:0; margin-bottom:0;\"", "span")
                    .replace("</p>", "</span>")
                    .replace("<b>", "<strong>")
                    .replace("</b>", "</strong>")
                    .replace("<i>", "<em>")
                    .replace("</i>", "</em>")
                    .replace("<u>", "<span style=\"text-decoration: underline;\">")
                    .replace("</u>", "</span>")
        }

        activity.bottomSheet.hideKeyboard()

        MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.messages_compose_confirm_title)
                .setMessage(R.string.messages_compose_confirm_text)
                .setPositiveButton(R.string.send) { _, _ ->
                    EdziennikTask.messageSend(App.profileId, recipients, subject.trim(), textHtml.trim()).enqueue(activity)
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

/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-7.
 */

package pl.szczodrzynski.edziennik.utils.managers

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StrikethroughSpan
import android.text.style.SubscriptSpan
import android.text.style.SuperscriptSpan
import android.text.style.UnderlineSpan
import android.widget.Button
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.mikepenz.iconics.typeface.IIcon
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.utils.TextInputKeyboardEdit
import pl.szczodrzynski.edziennik.utils.html.BetterHtml
import pl.szczodrzynski.edziennik.utils.managers.TextStylingManager.HtmlMode.*
import pl.szczodrzynski.edziennik.utils.span.BoldSpan
import pl.szczodrzynski.edziennik.utils.span.ItalicSpan

class TextStylingManager(private val app: App) {
    companion object {
        private const val TAG = "TextStylingManager"
    }

    private val paragraphBrRegex by lazy {
        "((?:<br>)+)</p>".toRegex()
    }

    enum class HtmlMode {
        /**
         * The default mode, suitable for fromHtml conversion.
         */
        ORIGINAL,

        /**
         * A more browser-compatible mode.
         */
        COMPATIBLE,

        /**
         * A simple, paragraph-stripped mode with \n instead of <br>.
         * The converted text has no HTML tags when no spans in source.
         */
        SIMPLE,

        /**
         * Markdown-compatible text mode.
         */
        MARKDOWN,
    }

    open class StylingConfigBase(
        val editText: TextInputKeyboardEdit,
        val htmlMode: HtmlMode = ORIGINAL,
    ) {
        var watchStyleChecked = true
        var watchSelectionChanged = true
    }

    class StylingConfig(
        editText: TextInputKeyboardEdit,
        val fontStyleGroup: MaterialButtonToggleGroup,
        val fontStyleClear: Button,
        val styles: List<Style>,
        val textHtml: TextView? = null,
        htmlMode: HtmlMode = ORIGINAL,
    ) : StylingConfigBase(editText, htmlMode) {
        data class Style(
            val button: MaterialButton,
            val spanClass: Class<*>,
            val icon: IIcon,
            @StringRes
            val hint: Int,
        ) {
            fun newInstance(): Any = spanClass.newInstance()
        }
    }

    fun attach(config: StylingConfig) {
        enableButtons(config, enable = false)
        config.editText.setOnFocusChangeListener { _, hasFocus ->
            enableButtons(config, enable = hasFocus)
        }

        config.styles.forEach {
            it.button.text = it.icon.character.toString()
            it.button.attachToastHint(it.hint)
        }

        config.fontStyleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            val style = config.styles.firstOrNull {
                it.button.id == checkedId
            } ?: return@addOnButtonCheckedListener
            onStyleChecked(config, style, isChecked)
        }

        config.fontStyleClear.setOnClickListener {
            onStyleClear(config)
        }

        config.editText.setSelectionChangedListener { selectionStart, selectionEnd ->
            onSelectionChanged(config, selectionStart, selectionEnd)
        }

        if (config.textHtml != null) {
            config.editText.addTextChangedListener {
                config.textHtml.text = getHtmlText(config)
            }
            config.textHtml.isVisible = true
            config.textHtml.text = getHtmlText(config)
        }

        /*b.fontStyleBold.shapeAppearanceModel = b.fontStyleBold.shapeAppearanceModel
            .toBuilder()
            .setBottomLeftCornerSize(0f)
            .build()
        b.fontStyleSuperscript.shapeAppearanceModel = b.fontStyleBold.shapeAppearanceModel
            .toBuilder()
            .setBottomRightCornerSize(0f)
            .build()
        b.fontStyleClear.shapeAppearanceModel = b.fontStyleClear.shapeAppearanceModel
            .toBuilder()
            .setTopLeftCornerSize(0f)
            .setTopRightCornerSize(0f)
            .build()*/
    }

    fun getHtmlText(config: StylingConfigBase, htmlMode: HtmlMode = config.htmlMode): String {
        val text = config.editText.text?.trimEnd() ?: return ""
        val spanned = SpannableStringBuilder(text)

        val toHtmlFlag = when (htmlMode) {
            COMPATIBLE -> HtmlCompat.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL
            else -> HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE
        }

        // apparently setting the spans to a different Spannable calls the original EditText's
        // onSelectionChanged with selectionStart=-1, which in effect unchecks the format toggles
        config.watchSelectionChanged = false

        // remove zero-length spans, as they seem to affect
        // the whole line when converted to HTML
        spanned.getSpans(0, spanned.length, Any::class.java).forEach {
            val spanStart = spanned.getSpanStart(it)
            val spanEnd = spanned.getSpanEnd(it)
            if (spanStart == spanEnd && it::class.java in BetterHtml.customSpanClasses)
                spanned.removeSpan(it)
        }

        var textHtml = when (htmlMode) {
            SIMPLE -> spanned
                .replaceSpan(BoldSpan::class.java, "<b>", "</b>")
                .replaceSpan(ItalicSpan::class.java, "<i>", "</i>")
                .replaceSpan(UnderlineSpan::class.java, "<u>", "</u>")
                .replaceSpan(StrikethroughSpan::class.java, "<s>", "</s>")
                .replaceSpan(SubscriptSpan::class.java, "<sub>", "</sub>")
                .replaceSpan(SuperscriptSpan::class.java, "<sup>", "</sup>")
                .toString()
            MARKDOWN -> spanned
                .replaceSpan(BoldSpan::class.java, "**", "**")
                .replaceSpan(ItalicSpan::class.java, "_", "_")
                .replaceSpan(UnderlineSpan::class.java, "__", "__")
                .replaceSpan(StrikethroughSpan::class.java, "~~", "~~")
                .toString()
            else -> HtmlCompat.toHtml(spanned, toHtmlFlag)
                .replace("\n", "")
                .replace(" dir=\"ltr\"", "")
                .replace("</b><b>", "")
                .replace("</i><i>", "")
                .replace("</u><u>", "")
                .replace("</sub><sub>", "")
                .replace("</sup><sup>", "")
                .replace("p style=\"margin-top:0; margin-bottom:0;\"", "p")
                .replace("<br></p>", "</p><br>")
                // replace multiple newlines so they convert fromHtml correctly
                // this should not be breaking with htmlCompatibleMode == true,
                // as line breaks cannot occur inside paragraphs with these flags
                .replace(paragraphBrRegex, "</p>$1")
        }

        config.watchSelectionChanged = true

        if (htmlMode == COMPATIBLE) {
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

    private fun onStyleChecked(
        config: StylingConfig,
        style: StylingConfig.Style,
        isChecked: Boolean,
    ) {
        if (!config.watchStyleChecked)
            return
        val span = style.newInstance()

        // see comments in getHtmlText()
        config.watchSelectionChanged = false
        if (isChecked)
            BetterHtml.applyFormat(span, config.editText)
        else
            BetterHtml.removeFormat(span, config.editText)
        config.watchSelectionChanged = true

        config.textHtml?.text = getHtmlText(config)
    }

    private fun onStyleClear(config: StylingConfig) {
        // shortened version on onStyleChecked(), removing all spans
        config.watchSelectionChanged = false
        BetterHtml.removeFormat(span = null, config.editText)
        config.watchSelectionChanged = true
        config.textHtml?.text = getHtmlText(config)
        // force update of text style toggle states
        onSelectionChanged(config, config.editText.selectionStart, config.editText.selectionEnd)
    }

    private fun onSelectionChanged(config: StylingConfig, selectionStart: Int, selectionEnd: Int) {
        if (!config.watchSelectionChanged)
            return
        val spanned = config.editText.text ?: return
        val spans = spanned.getSpans(selectionStart, selectionEnd, Any::class.java).mapNotNull {
            if (it::class.java !in BetterHtml.customSpanClasses)
                return@mapNotNull null
            val spanStart = spanned.getSpanStart(it)
            val spanEnd = spanned.getSpanEnd(it)
            // remove 0-length spans after navigating out of them
            if (spanStart == spanEnd)
                spanned.removeSpan(it)
            else if (spanned.getSpanFlags(it) hasSet Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spanned.setSpan(it, spanStart, spanEnd, Spanned.SPAN_EXCLUSIVE_INCLUSIVE)

            // names are helpful here
            val isNotAfterWord = selectionEnd <= spanEnd
            val isSelectionInWord = selectionStart != selectionEnd && selectionStart >= spanStart
            val isCursorInWord = selectionStart == selectionEnd && selectionStart > spanStart
            val isChecked = (isCursorInWord || isSelectionInWord) && isNotAfterWord
            if (isChecked) it::class.java else null
        }
        config.watchStyleChecked = false
        config.styles.forEach {
            it.button.isChecked = it.spanClass in spans
        }
        config.watchStyleChecked = true
    }

    private fun enableButtons(config: StylingConfig, enable: Boolean) {
        config.fontStyleClear.isEnabled = enable
        config.styles.forEach {
            it.button.isEnabled = enable
        }
    }
}

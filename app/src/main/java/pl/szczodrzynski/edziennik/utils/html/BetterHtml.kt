/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-17.
 */

package pl.szczodrzynski.edziennik.utils.html

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.Spanned.*
import android.text.style.*
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.graphics.ColorUtils
import androidx.core.text.HtmlCompat
import pl.szczodrzynski.edziennik.dp
import pl.szczodrzynski.edziennik.getWordBounds
import pl.szczodrzynski.edziennik.resolveAttr
import pl.szczodrzynski.edziennik.utils.span.*
import pl.szczodrzynski.navlib.blendColors

object BetterHtml {

    @JvmStatic
    fun fromHtml(context: Context, html: String): Spanned {
        val hexPattern = "(#[a-fA-F0-9]{6})"
        val colorRegex = "(?:color=\"$hexPattern\")|(?:style=\"color: ?${hexPattern})"
            .toRegex(RegexOption.IGNORE_CASE)

        var text = html
            .replace("\\[META:[A-z0-9]+;[0-9-]+]".toRegex(), "")
            .replace("background-color: ?$hexPattern;".toRegex(), "")

        val colorBackground = android.R.attr.colorBackground.resolveAttr(context)
        val textColorPrimary = android.R.attr.textColorPrimary.resolveAttr(context) and 0xffffff

        colorRegex.findAll(text).forEach { result ->
            val group = result.groups.drop(1).firstOrNull { it != null } ?: return@forEach

            val color = Color.parseColor(group.value)
            var newColor = 0xff000000.toInt() or color

            var blendAmount = 1
            var numIterations = 0

            while (numIterations < 100 && ColorUtils.calculateContrast(
                    colorBackground,
                    newColor
                ) < 4.5f
            ) {
                blendAmount += 2
                newColor = blendColors(color, blendAmount shl 24 or textColorPrimary)
                numIterations++
            }

            text = text.replaceRange(group.range, "#" + (newColor and 0xffffff).toString(16))
        }

        /*val olRegex = """<ol>(.+?)</\s*?ol>"""
                .toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        olRegex.findAll(text).forEach {
            text.replaceRange(
                    it.range,
                    text.slice(it.range).replace("li>", "_li>")
            )
        }*/

        @Suppress("DEPRECATION")
        val htmlSpannable = HtmlCompat.fromHtml(
            text,
            HtmlCompat.FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM or HtmlCompat.FROM_HTML_SEPARATOR_LINE_BREAK_LIST or HtmlCompat.FROM_HTML_SEPARATOR_LINE_BREAK_DIV,
            null,
            LiTagHandler()
        )

        val spanned = SpannableStringBuilder(htmlSpannable)
        spanned.getSpans(0, spanned.length, Any::class.java).forEach {
            val spanStart = spanned.getSpanStart(it)
            val spanEnd = spanned.getSpanEnd(it)
            val spanFlags = spanned.getSpanFlags(it)

            val newSpan: Any? = when (it) {
                is BulletSpan -> ImprovedBulletSpan(
                    bulletRadius = 3.dp,
                    startWidth = 24.dp,
                    gapWidth = 8.dp
                )
                is StyleSpan -> when (it.style) {
                    Typeface.BOLD -> BoldSpan()
                    Typeface.ITALIC -> ItalicSpan()
                    else -> null
                }
                is SubscriptSpan -> SubscriptSizeSpan(size = 10, dip = true)
                is SuperscriptSpan -> SuperscriptSizeSpan(size = 10, dip = true)
                else -> null
            }

            if (newSpan != null) {
                spanned.removeSpan(it)
                spanned.setSpan(newSpan, spanStart, spanEnd, spanFlags)
            }
        }

        return spanned
    }

    fun applyFormat(span: Any, editText: AppCompatEditText) {
        applyFormat(span, editText.text ?: return, editText.selectionStart, editText.selectionEnd)
    }

    fun removeFormat(span: Any, editText: AppCompatEditText) {
        removeFormat(span, editText.text ?: return, editText.selectionStart, editText.selectionEnd)
    }

    fun applyFormat(span: Any, spanned: Editable, selectionStart: Int, selectionEnd: Int) {
        if (selectionStart == -1 || selectionEnd == -1) return
        val cursorOnly = selectionStart == selectionEnd

        val wordBounds = spanned.getWordBounds(selectionStart, onlyInWord = true)
        if (cursorOnly && wordBounds != null) {
            // use the detected word bounds instead of cursor/selection
            val (start, end) = wordBounds
            spanned.setSpan(span, start, end, SPAN_EXCLUSIVE_INCLUSIVE)
        } else {
            val spanFlags = if (cursorOnly)
                SPAN_INCLUSIVE_INCLUSIVE
            else
                SPAN_EXCLUSIVE_INCLUSIVE
            spanned.setSpan(span, selectionStart, selectionEnd, spanFlags)
        }
    }

    fun removeFormat(span: Any, spanned: Editable, selectionStart: Int, selectionEnd: Int) {
        if (selectionStart == -1 || selectionEnd == -1) return
        val cursorOnly = selectionStart == selectionEnd

        spanned.getSpans(selectionStart, selectionEnd, span.javaClass).forEach {
            val spanStart = spanned.getSpanStart(it)
            val spanEnd = spanned.getSpanEnd(it)
            val wordBounds = spanned.getWordBounds(selectionStart, onlyInWord = true)

            val (newSpanStart, newSpanEnd, newSpanFlags) = when {
                !cursorOnly -> {
                    // cut the selected range out of the span
                    Triple(selectionStart, selectionEnd, SPAN_EXCLUSIVE_INCLUSIVE)
                }
                wordBounds == null -> {
                    // this allows to change spans mid-word - EXCLUSIVE so the style does
                    // not apply to characters typed later
                    // it's set back to INCLUSIVE when the cursor enters the word again
                    // (onSelectionChanged)
                    Triple(selectionStart, selectionEnd, SPAN_EXCLUSIVE_EXCLUSIVE)
                }
                else /* wordBounds != null */ -> {
                    // a word is selected, slice the span in two
                    Triple(wordBounds.first, wordBounds.second, SPAN_EXCLUSIVE_INCLUSIVE)
                }
            }

            // remove the existing span
            spanned.removeSpan(it)
            // reapply the span wherever needed
            // use "it" and "span" only once, so they don't replace the applied range
            if (spanStart < newSpanStart)
                spanned.setSpan(it, spanStart, newSpanStart, newSpanFlags)
            if (spanEnd > newSpanEnd)
                spanned.setSpan(span, newSpanEnd, spanEnd, newSpanFlags)
        }
    }
}

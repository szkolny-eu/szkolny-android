/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-17.
 */

package pl.szczodrzynski.edziennik.utils.html

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import androidx.core.graphics.ColorUtils
import pl.szczodrzynski.edziennik.dp
import pl.szczodrzynski.edziennik.resolveAttr
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

            while (numIterations < 100 && ColorUtils.calculateContrast(colorBackground, newColor) < 4.5f) {
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
        val htmlSpannable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(
                    text,
                    Html.FROM_HTML_SEPARATOR_LINE_BREAK_LIST_ITEM or Html.FROM_HTML_SEPARATOR_LINE_BREAK_LIST or Html.FROM_HTML_SEPARATOR_LINE_BREAK_DIV,
                    null,
                    LiTagHandler()
            )
        } else {
            Html.fromHtml(text, null, LiTagHandler())
        }

        val spannableBuilder = SpannableStringBuilder(htmlSpannable)
        val bulletSpans = spannableBuilder.getSpans(0, spannableBuilder.length, BulletSpan::class.java)
        bulletSpans.forEach {
            val start = spannableBuilder.getSpanStart(it)
            val end = spannableBuilder.getSpanEnd(it)
            spannableBuilder.removeSpan(it)
            spannableBuilder.setSpan(
                    ImprovedBulletSpan(bulletRadius = 3.dp, startWidth = 24.dp, gapWidth = 8.dp),
                    start,
                    end,
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE
            )
        }

        return spannableBuilder
    }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.ext

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.CharacterStyle
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.mikepenz.materialdrawer.holder.StringHolder

fun CharSequence?.isNotNullNorEmpty(): Boolean {
    return this != null && this.isNotEmpty()
}

fun CharSequence?.isNotNullNorBlank(): Boolean {
    return this != null && this.isNotBlank()
}

/**
 * `   The quick BROWN_fox Jumps OveR THE       LAZy-DOG.   `
 *
 * converts to
 *
 * `The Quick Brown_fox Jumps Over The Lazy-Dog.`
 */
fun String?.fixName(): String {
    return this?.fixWhiteSpaces()?.toProperCase() ?: ""
}

/**
 * `The quick BROWN_fox Jumps OveR THE       LAZy-DOG.`
 *
 * converts to
 *
 * `The Quick Brown_fox Jumps Over The       Lazy-Dog.`
 */
fun String.toProperCase(): String = changeStringCase(this)

/**
 * `John Smith` -> `Smith John`
 *
 * `JOHN SMith` -> `SMith JOHN`
 */
fun String.swapFirstLastName(): String {
    return this.split(" ").let {
        if (it.size > 1)
            it[1]+" "+it[0]
        else
            it[0]
    }
}

fun String.splitName(): Pair<String, String>? {
    return this.split(" ").let {
        if (it.size >= 2) Pair(it[0], it[1])
        else null
    }
}

fun changeStringCase(s: String): String {
    val delimiters = " '-/"
    val sb = StringBuilder()
    var capNext = true
    for (ch in s.toCharArray()) {
        var c = ch
        c = if (capNext)
            Character.toUpperCase(c)
        else
            Character.toLowerCase(c)
        sb.append(c)
        capNext = delimiters.indexOf(c) >= 0
    }
    return sb.toString()
}

fun buildFullName(firstName: String?, lastName: String?): String {
    return "$firstName $lastName".fixName()
}

fun String.getShortName(): String {
    return split(" ").let {
        if (it.size > 1)
            "${it[0]} ${it[1][0]}."
        else
            it[0]
    }
}

/**
 * "John Smith"             -> "JS"
 *
 * "JOHN SMith"             -> "JS"
 *
 * "John"                   -> "J"
 *
 * "John "                  -> "J"
 *
 * "John     Smith      "   -> "JS"
 *
 * " "                      -> ""
 *
 * "  "                     -> ""
 */
fun String?.getNameInitials(): String {
    if (this.isNullOrBlank()) return ""
    return this.uppercase().fixWhiteSpaces().split(" ").take(2).map { it[0] }.joinToString("")
}

operator fun MatchResult.get(group: Int): String {
    if (group >= groupValues.size)
        return ""
    return groupValues[group]
}

fun String.fixWhiteSpaces() = buildString(length) {
    var wasWhiteSpace = true
    for (c in this@fixWhiteSpaces) {
        if (c.isWhitespace()) {
            if (!wasWhiteSpace) {
                append(c)
                wasWhiteSpace = true
            }
        } else {
            append(c)
            wasWhiteSpace = false
        }
    }
}.trimEnd()

fun CharSequence?.asColoredSpannable(colorInt: Int): Spannable {
    val spannable = SpannableString(this)
    spannable.setSpan(ForegroundColorSpan(colorInt), 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    return spannable
}
fun CharSequence?.asStrikethroughSpannable(): Spannable {
    val spannable = SpannableString(this)
    spannable.setSpan(StrikethroughSpan(), 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    return spannable
}
fun CharSequence?.asItalicSpannable(): Spannable {
    val spannable = SpannableString(this)
    spannable.setSpan(StyleSpan(Typeface.ITALIC), 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    return spannable
}
fun CharSequence?.asBoldSpannable(): Spannable {
    val spannable = SpannableString(this)
    spannable.setSpan(StyleSpan(Typeface.BOLD), 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    return spannable
}
fun CharSequence.asSpannable(
    vararg spans: CharacterStyle,
    substring: CharSequence? = null,
    ignoreCase: Boolean = false,
    ignoreDiacritics: Boolean = false
): Spannable {
    val spannable = SpannableString(this)
    substring?.let { substr ->
        val string = if (ignoreDiacritics)
            this.cleanDiacritics()
        else
            this
        val search = if (ignoreDiacritics)
            substr.cleanDiacritics()
        else
            substr.toString()

        var index = 0
        do {
            index = string.indexOf(
                string = search,
                startIndex = index,
                ignoreCase = ignoreCase
            )

            if (index >= 0) {
                spans.forEach {
                    spannable.setSpan(
                        CharacterStyle.wrap(it),
                        index,
                        index + substring.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                index += substring.length.coerceAtLeast(1)
            }
        } while (index >= 0)

    } ?: spans.forEach {
        spannable.setSpan(it, 0, spannable.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    return spannable
}

fun CharSequence.cleanDiacritics(): String {
    val nameClean = StringBuilder()
    forEach {
        val ch = when (it) {
            'ż' -> 'z'
            'ó' -> 'o'
            'ł' -> 'l'
            'ć' -> 'c'
            'ę' -> 'e'
            'ś' -> 's'
            'ą' -> 'a'
            'ź' -> 'z'
            'ń' -> 'n'
            else -> it
        }
        nameClean.append(ch)
    }
    return nameClean.toString()
}

operator fun StringBuilder.plusAssign(str: String?) {
    this.append(str)
}

val String.firstLettersName: String
    get() {
        var nameShort = ""
        this.split(" ").forEach {
            if (it.isBlank())
                return@forEach
            nameShort += it[0].lowercase()
        }
        return nameShort
    }

fun CharSequence.replaceSpanned(oldValue: String, newValue: CharSequence, ignoreCase: Boolean = false): CharSequence {
    var seq = this
    var index = seq.indexOf(oldValue, ignoreCase = ignoreCase)
    while (index != -1) {
        val sb = SpannableStringBuilder()
        sb.appendRange(seq, 0, index)
        sb.append(newValue)
        sb.appendRange(seq, index + oldValue.length, seq.length)
        seq = sb
        index = seq.indexOf(oldValue, startIndex = index + 1, ignoreCase = ignoreCase)
    }
    return seq
}

fun SpannableStringBuilder.replaceSpan(spanClass: Class<*>, prefix: CharSequence, suffix: CharSequence): SpannableStringBuilder {
    getSpans(0, length, spanClass).forEach {
        val spanStart = getSpanStart(it)
        insert(spanStart, prefix)
        val spanEnd = getSpanEnd(it)
        insert(spanEnd, suffix)
    }
    return this
}

fun SpannableStringBuilder.appendText(text: CharSequence): SpannableStringBuilder {
    append(text)
    return this
}
fun SpannableStringBuilder.appendSpan(text: CharSequence, what: Any, flags: Int): SpannableStringBuilder {
    val start: Int = length
    append(text)
    setSpan(what, start, length, flags)
    return this
}

fun joinNotNullStrings(delimiter: String = "", vararg parts: String?): String {
    var first = true
    val sb = StringBuilder()
    for (part in parts) {
        if (part == null)
            continue
        if (!first)
            sb += delimiter
        first = false
        sb += part
    }
    return sb.toString()
}

fun String.notEmptyOrNull(): String? {
    return if (isEmpty())
        null
    else
        this
}

fun Context.plural(@PluralsRes resId: Int, value: Int): String = resources.getQuantityString(resId, value, value)

fun String.copyToClipboard(context: Context) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = ClipData.newPlainText("Tekst", this)
    clipboard.setPrimaryClip(clipData)
}

fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

fun CharSequence.getWordBounds(position: Int, onlyInWord: Boolean = false): Pair<Int, Int>? {
    if (length == 0)
        return null

    // only if cursor between letters
    if (onlyInWord) {
        if (position < 1)
            return null
        if (position == length)
            return null

        val charBefore = this[position - 1]
        if (!charBefore.isLetterOrDigit())
            return null
        val charAfter = this[position]
        if (!charAfter.isLetterOrDigit())
            return null
    }

    var rangeStart = substring(0 until position).indexOfLast { !it.isLetterOrDigit() }
    if (rangeStart == -1) // no whitespace, set to first index
        rangeStart = 0
    else // cut the leading whitespace
        rangeStart += 1

    var rangeEnd = substring(position).indexOfFirst { !it.isLetterOrDigit() }
    if (rangeEnd == -1) // no whitespace, set to last index
        rangeEnd = length
    else // append the substring offset
        rangeEnd += position

    if (!onlyInWord && rangeStart == rangeEnd)
        return null
    return rangeStart to rangeEnd
}

fun Int.toStringHolder() = StringHolder(this)
fun CharSequence.toStringHolder() = StringHolder(this)

fun @receiver:StringRes Int.resolveString(context: Context) = context.getString(this)

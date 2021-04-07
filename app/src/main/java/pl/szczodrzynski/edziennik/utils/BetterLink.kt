/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-18.
 */

@file:Suppress("INACCESSIBLE_TYPE")

package pl.szczodrzynski.edziennik.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.URLSpan
import android.text.util.Linkify
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.view.menu.MenuPopupHelper
import pl.szczodrzynski.edziennik.Intent
import pl.szczodrzynski.edziennik.copyToClipboard
import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.get
import pl.szczodrzynski.edziennik.getTextPosition
import pl.szczodrzynski.edziennik.utils.models.Date

@SuppressLint("RestrictedApi")
object BetterLink {

    private fun MenuBuilder.setTitle(title: CharSequence): MenuBuilder {
        this::class.java.getDeclaredMethod("setHeaderTitleInt", CharSequence::class.java).let {
            it.isAccessible = true
            it.invoke(this, title)
        }
        return this
    }

    private fun createUrlItems(menu: MenuBuilder, context: Context, url: String) {
        menu.setTitle(url)
        menu.add(
            1,
            1,
            2,
            "Otwórz w przeglądarce"
        ).setOnMenuItemClickListener {
            Utils.openUrl(context, url)
            true
        }
    }

    private fun createMailtoItems(menu: MenuBuilder, context: Context, url: String) {
        menu.add(
            1,
            20,
            2,
            "Napisz e-mail"
        ).setOnMenuItemClickListener {
            Utils.openUrl(context, url)
            true
        }
    }

    private fun createDateItems(menu: MenuBuilder, context: Context, date: Date?) {
        date ?: return
        menu.setTitle(date.formattedString)
        menu.add(
            1,
            10,
            2,
            "Utwórz wydarzenie"
        ).setOnMenuItemClickListener {
            val intent = Intent(
                Intent.ACTION_MAIN,
                "action" to "createManualEvent",
                "eventDate" to date.stringY_m_d
            )
            context.sendBroadcast(intent)
            true
        }
    }

    private fun onClickListener(
        view: TextView,
        span: BetterLinkMovementMethod.ClickableSpanWithText,
        onActionSelected: (() -> Unit)?
    ): Boolean {
        val c = view.context

        val spanned = view.text as Spanned
        val start = spanned.getSpanStart(span.span())
        val end = spanned.getSpanEnd(span.span())

        val parent = view.rootView.findViewById<ViewGroup>(android.R.id.content)
        val parentLocation = intArrayOf(0, 0)
        parent.getLocationOnScreen(parentLocation)

        val rect = view.getTextPosition(start..end)

        val popupView = View(c)
        popupView.layoutParams = ViewGroup.LayoutParams(rect.width(), rect.height())
        popupView.setBackgroundColor(Color.TRANSPARENT)

        parent.addView(popupView)

        popupView.x = rect.left.toFloat() - parentLocation[0]
        popupView.y = rect.top.toFloat() - parentLocation[1]

        val menu = MenuBuilder(c)
        val helper = MenuPopupHelper(c, menu, popupView)
        val popup = helper.popup

        val spanUrl = span.text()
        val spanText = spanUrl.substringAfter(":")

        //goToTimetableItem = menu.add(1, 11, 3, "Idź do planu lekcji")

        // create appropriate items for spans
        when {
            spanUrl.startsWith("mailto:") -> createMailtoItems(menu, c, spanUrl)
            spanUrl.startsWith("dateYmd:") -> createDateItems(menu, c, parseDateYmd(spanText))
            spanUrl.startsWith("dateDmy:") -> createDateItems(menu, c, parseDateDmy(spanText))
            spanUrl.startsWith("dateAbs:") -> createDateItems(menu, c, parseDateAbs(spanText))
            spanUrl.startsWith("dateRel:") -> createDateItems(menu, c, parseDateRel(spanText))
            else -> createUrlItems(menu, c, spanUrl)
        }
        menu.add(1, 1000, 1000, "Kopiuj tekst").setOnMenuItemClickListener {
            spanText.copyToClipboard(c)
            true
        }

        helper.setOnDismissListener { parent.removeView(popupView) }

        popup::class.java.getDeclaredField("mShowTitle").let {
            it.isAccessible = true
            it.set(popup, true)
        }
        helper::class.java.getDeclaredMethod(
            "showPopup",
            Int::class.java,
            Int::class.java,
            Boolean::class.java,
            Boolean::class.java
        ).let {
            it.isAccessible = true
            it.invoke(helper, 0, 0, false, true)
        }
        return true
    }

    fun attach(
        textView: TextView,
        onActionSelected: (() -> Unit)? = null
    ) {
        textView.autoLinkMask = Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES

        BetterLinkMovementMethod
            .linkify(textView.autoLinkMask, textView)
            .setOnLinkClickListener { view, span ->
                onClickListener(view, span, onActionSelected)
            }

        val spanned = textView.text as? Spannable ?: SpannableString(textView.text)

        Regexes.LINKIFY_DATE_YMD.findAll(textView.text).forEach { match ->
            val span = URLSpan("dateYmd:" + match.value)
            spanned.setSpan(
                span,
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        Regexes.LINKIFY_DATE_DMY.findAll(textView.text).forEach { match ->
            val span = URLSpan("dateDmy:" + match.value)
            spanned.setSpan(
                span,
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        Regexes.LINKIFY_DATE_ABSOLUTE.findAll(textView.text).forEach { match ->
            val span = URLSpan("dateAbs:" + match.value)
            spanned.setSpan(
                span,
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        Regexes.LINKIFY_DATE_RELATIVE.findAll(textView.text).forEach { match ->
            val span = URLSpan("dateRel:" + match.value)
            spanned.setSpan(
                span,
                match.range.first,
                match.range.last + 1,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private val monthNames =
        listOf("sty", "lut", "mar", "kwi", "maj", "cze", "lip", "sie", "wrz", "paź", "lis", "gru")

    private fun parseDateYmd(text: String): Date? {
        return Regexes.LINKIFY_DATE_YMD.find(text)?.let {
            val year = it[1].toIntOrNull() ?: Date.getToday().year
            val month = it[2].toIntOrNull() ?: 1
            val day = it[3].toIntOrNull() ?: 1
            Date(year, month, day)
        }
    }

    private fun parseDateDmy(text: String): Date? {
        return Regexes.LINKIFY_DATE_DMY.find(text)?.let {
            val day = it[1].toIntOrNull() ?: 1
            val month = it[2].toIntOrNull() ?: 1
            var year = it[3].toIntOrNull() ?: Date.getToday().year
            if (year < 50)
                year += 2000
            Date(year, month, day)
        }
    }

    private fun parseDateAbs(text: String): Date? {
        return Regexes.LINKIFY_DATE_ABSOLUTE.find(text)?.let {
            val year = it[3].toIntOrNull() ?: Date.getToday().year
            val month = monthNames.indexOf(it[2]) + 1
            val day = it[1].toIntOrNull() ?: 1
            Date(year, month.coerceAtLeast(1), day)
        }
    }

    private fun parseDateRel(text: String): Date? {
        return Regexes.LINKIFY_DATE_RELATIVE.find(text)?.let {
            val date = Date.getToday()

            val amount = it[1].toIntOrNull() ?: 1
            val unitInDays = when (it[2]) {
                "dni", "dzień" -> 1
                "tydzień", "tygodnie" -> 7
                else -> 1
            }

            date.stepForward(0, 0, amount * unitInDays)
        }
    }
}

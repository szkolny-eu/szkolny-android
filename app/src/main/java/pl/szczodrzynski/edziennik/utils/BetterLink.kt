/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-18.
 */

@file:Suppress("INACCESSIBLE_TYPE")

package pl.szczodrzynski.edziennik.utils

import android.annotation.SuppressLint
import android.graphics.Color
import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.URLSpan
import android.text.util.Linkify
import android.view.MenuItem
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

object BetterLink {

    @SuppressLint("RestrictedApi")
    fun attach(textView: TextView, onActionSelected: (() -> Unit)? = null) {
        textView.autoLinkMask = Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES
        BetterLinkMovementMethod.linkify(textView.autoLinkMask, textView).setOnLinkClickListener { v, span: BetterLinkMovementMethod.ClickableSpanWithText ->
            val url = span.text()
            val c = v.context

            val s = v.text as Spanned
            val start = s.getSpanStart(span.span())
            val end = s.getSpanEnd(span.span())

            val parent = v.rootView.findViewById<ViewGroup>(android.R.id.content)
            val parentLocation = intArrayOf(0, 0)
            parent.getLocationOnScreen(parentLocation)

            val rect = textView.getTextPosition(start..end)

            val view = View(c)
            view.layoutParams = ViewGroup.LayoutParams(rect.width(), rect.height())
            view.setBackgroundColor(Color.TRANSPARENT)

            parent.addView(view)

            view.x = rect.left.toFloat() - parentLocation[0]
            view.y = rect.top.toFloat() - parentLocation[1]

            val menu = MenuBuilder(c)
            val helper = MenuPopupHelper(c, menu, view)
            val popup = helper.popup

            var menuTitle = url.substringAfter(":")
            var date: Date? = null

            var urlItem: MenuItem? = null
            var createEventItem: MenuItem? = null
            //var goToTimetableItem: MenuItem? = null // TODO 2020-03-19: implement this
            var mailItem: MenuItem? = null
            var copyItem: MenuItem? = null

            when {
                url.startsWith("mailto:") -> {
                    mailItem = menu.add(1, 20, 2, "Napisz e-mail")
                }
                url.startsWith("dateYmd:") -> {
                    createEventItem = menu.add(1, 10, 2, "Utwórz wydarzenie")
                    //goToTimetableItem = menu.add(1, 11, 3, "Idź do planu lekcji")
                    date = parseDateYmd(menuTitle)
                }
                url.startsWith("dateDmy:") -> {
                    createEventItem = menu.add(1, 10, 2, "Utwórz wydarzenie")
                    //goToTimetableItem = menu.add(1, 11, 3, "Idź do planu lekcji")
                    date = parseDateDmy(menuTitle)
                }
                url.startsWith("dateAbs:") -> {
                    createEventItem = menu.add(1, 10, 2, "Utwórz wydarzenie")
                    //goToTimetableItem = menu.add(1, 11, 3, "Idź do planu lekcji")
                    date = parseDateAbs(menuTitle)
                }
                url.startsWith("dateRel:") -> {
                    createEventItem = menu.add(1, 10, 2, "Utwórz wydarzenie")
                    //goToTimetableItem = menu.add(1, 11, 3, "Idź do planu lekcji")
                    date = parseDateRel(menuTitle)
                }
                else -> {
                    urlItem = menu.add(1, 1, 2, "Otwórz w przeglądarce")
                    menuTitle = url
                }
            }
            copyItem = menu.add(1, 1000, 1000, "Kopiuj tekst")

            helper.setOnDismissListener { parent.removeView(view) }

            urlItem?.setOnMenuItemClickListener { Utils.openUrl(c, url); true }
            mailItem?.setOnMenuItemClickListener { Utils.openUrl(c, url); true }
            copyItem?.setOnMenuItemClickListener { menuTitle.copyToClipboard(c); true }
            createEventItem?.setOnMenuItemClickListener {
                onActionSelected?.invoke()
                val intent = Intent(
                        android.content.Intent.ACTION_MAIN,
                        "action" to "createManualEvent",
                        "eventDate" to date?.stringY_m_d
                )
                c.sendBroadcast(intent)
                true
            }

            menu::class.java.getDeclaredMethod("setHeaderTitleInt", CharSequence::class.java).let {
                it.isAccessible = true
                it.invoke(menu, menuTitle)
            }
            popup::class.java.getDeclaredField("mShowTitle").let {
                it.isAccessible = true
                it.set(popup, true)
            }
            helper::class.java.getDeclaredMethod("showPopup", Int::class.java, Int::class.java, Boolean::class.java, Boolean::class.java).let {
                it.isAccessible = true
                it.invoke(helper, 0, 0, false, true)
            }
            true
        }

        val spanned = textView.text as? Spannable ?: {
            SpannableString(textView.text)
        }()

        Regexes.LINKIFY_DATE_YMD.findAll(textView.text).forEach { match ->
            val span = URLSpan("dateYmd:" + match.value)
            spanned.setSpan(span, match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        Regexes.LINKIFY_DATE_DMY.findAll(textView.text).forEach { match ->
            val span = URLSpan("dateDmy:" + match.value)
            spanned.setSpan(span, match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        Regexes.LINKIFY_DATE_ABSOLUTE.findAll(textView.text).forEach { match ->
            val span = URLSpan("dateAbs:" + match.value)
            spanned.setSpan(span, match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        Regexes.LINKIFY_DATE_RELATIVE.findAll(textView.text).forEach { match ->
            val span = URLSpan("dateRel:" + match.value)
            spanned.setSpan(span, match.range.first, match.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        //Linkify.addLinks(textView, LINKIFY_DATE_ABSOLUTE.toPattern(), "dateAbs:")
        //Linkify.addLinks(textView, LINKIFY_DATE_RELATIVE.toPattern(), "dateRel:")
    }

    private val monthNames = listOf("sty", "lut", "mar", "kwi", "maj", "cze", "lip", "sie", "wrz", "paź", "lis", "gru")

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

            date.stepForward(0, 0, amount*unitInDays)
        }
    }
}

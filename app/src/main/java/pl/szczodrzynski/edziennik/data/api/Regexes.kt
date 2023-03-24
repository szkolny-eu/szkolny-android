/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-6.
 */

package pl.szczodrzynski.edziennik.data.api

import kotlin.text.RegexOption.DOT_MATCHES_ALL
import kotlin.text.RegexOption.IGNORE_CASE

object Regexes {
    val STYLE_CSS_COLOR by lazy {
        """color: (\w+);?""".toRegex()
    }

    val NOT_DIGITS by lazy {
        """[^0-9]""".toRegex()
    }

    val HTML_BR by lazy {
        """<br\s?/?>""".toRegex()
    }

    val MESSAGE_META by lazy {
        """^\[META:([A-z0-9-&=]+)]""".toRegex()
    }

    val HTML_INPUT_HIDDEN by lazy {
        """<input .*?type="hidden".+?>""".toRegex()
    }
    val HTML_INPUT_NAME by lazy {
        """name="(.+?)"""".toRegex()
    }
    val HTML_INPUT_VALUE by lazy {
        """value="(.+?)"""".toRegex()
    }
    val HTML_CSRF_TOKEN by lazy {
        """name="csrf-token" content="([A-z0-9=+/\-_]+?)"""".toRegex()
    }
    val HTML_FORM_ACTION by lazy {
        """<form .*?action="(.+?)"""".toRegex()
    }
    val HTML_RECAPTCHA_KEY by lazy {
        """data-sitekey="(.+?)"""".toRegex()
    }



    val MOBIDZIENNIK_GRADES_SUBJECT_NAME by lazy {
        """<div.*?>\n*\s*(.+?)\s*\n*(?:<.*?)??</div>""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_GRADES_COLOR by lazy {
        """background-color:([#A-Fa-f0-9]+);""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_GRADES_CATEGORY by lazy {
        """>&nbsp;(.+?):</span>""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_GRADES_CLASS_AVERAGE by lazy {
        """Średnia ocen:.*<strong>([0-9]*\.?[0-9]*)</strong>""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_GRADES_ADDED_DATE by lazy {
        """Wpisano:.*<strong>.+?,\s([0-9]+)\s(.+?)\s([0-9]{4}),\sgodzina\s([0-9:]+)</strong>""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_GRADES_COUNT_TO_AVG by lazy {
        """Liczona do średniej:.*?<strong>nie<br/?></strong>""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_GRADES_DETAILS by lazy {
        """<strong.*?>(.+?)</strong>.*?<sup>.+?</sup>.*?(?:<small>\((.+?)\)</small>.*?)?<span>.*?Wartość oceny:.*?<strong>([0-9.]+)</strong>.*?Wpisał\(a\):.*?<strong>(.+?)</strong>.*?(?:Komentarz:.*?<strong>(.+?)</strong>)?</span>""".toRegex(DOT_MATCHES_ALL)
    }

    val MOBIDZIENNIK_EVENT_TYPE by lazy {
        """\(([0-9A-ząęóżźńśłć]*?)\)$""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_LUCKY_NUMBER by lazy {
        """class="szczesliwy_numerek".*?>0?([0-9]+)/?0?([0-9]+)?</a>""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_CLASS_CALENDAR by lazy {
        """events: (.+),$""".toRegex(RegexOption.MULTILINE)
    }

    val MOBIDZIENNIK_WEB_ATTACHMENT by lazy {
        """href="https://.+?\.mobidziennik.pl/.+?&(?:amp;)?zalacznik(_rozwiazania)?=([0-9]+)".+?>(.+?)(?: <small.+?\(([0-9.]+)\s(M|K|G|)B\)</small>)?</a>""".toRegex()
    }

    val MOBIDZIENNIK_MESSAGE_READ_DATE by lazy {
        """czas przeczytania:.+?,\s([0-9]+)\s(.+?)\s([0-9]{4}),\sgodzina\s([0-9:]+)""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_MESSAGE_SENT_READ_DATE by lazy {
        """.+?,\s([0-9]+)\s(.+?)\s([0-9]{4}),\sgodzina\s([0-9:]+)""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_MESSAGE_SENT_READ_BY by lazy {
        """([0-9]+)/([0-9]+)""".toRegex()
    }

    val MOBIDZIENNIK_MESSAGE_RECIPIENTS_JSON by lazy {
        """odbiorcy: (\[.+?]),${'$'}""".toRegex(RegexOption.MULTILINE)
    }

    val MOBIDZIENNIK_ACCOUNT_EMAIL by lazy {
        """name="email" value="(.+?@.+?\..+?)"""".toRegex(DOT_MATCHES_ALL)
    }


    val MOBIDZIENNIK_ATTENDANCE_TYPES by lazy {
        """Legenda:.+?normal;">(.+?)</span>""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_ATTENDANCE_TABLE by lazy {
        """<table .+?id="obecnosci_tabela">(.+?)</table>""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_ATTENDANCE_LESSON_COUNT by lazy {
        """rel="([0-9-]{10})" colspan="([0-9]+)"""".toRegex()
    }
    val MOBIDZIENNIK_ATTENDANCE_ENTRIES by lazy {
        """font-size:.+?class=".*?">(.*?)</td>""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_ATTENDANCE_COLUMNS by lazy {
        """<tr><td class="border-right1".+?/td>(.+?)</tr>""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_ATTENDANCE_COLUMN by lazy {
        """(<td.+?>)(.*?)</td>""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_ATTENDANCE_COLUMN_SPAN by lazy {
        """colspan="(\d+)"""".toRegex()
    }
    val MOBIDZIENNIK_ATTENDANCE_RANGE by lazy {
        """<span>([0-9:]+) - .+? (.+?)</span></a>""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_ATTENDANCE_LESSON by lazy {
        """<strong>(.+?)</strong>\s*<small>\s*\((.+?),\s*(.+?)\)""".toRegex(DOT_MATCHES_ALL)
    }

    val MOBIDZIENNIK_MOBILE_HOMEWORK_ROW by lazy {
        """class="rowRolling">(.+?</div>\s*</td>)""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_MOBILE_HOMEWORK_ITEM by lazy {
        """<p><b>(.+?):</b>\s*(.+?)\s*</p>""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_MOBILE_HOMEWORK_BODY by lazy {
        """Treść:</b>(.+?)<p><b>""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_MOBILE_HOMEWORK_ID by lazy {
        """name="id_zadania" value="([0-9]+)"""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_MOBILE_HOMEWORK_ATTACHMENT by lazy {
        """zalacznik(_zadania)?=([0-9]+)'.+?word-break">(.+?)</td>""".toRegex(DOT_MATCHES_ALL)
    }

    val MOBIDZIENNIK_WEB_HOMEWORK_ADDED_DATE by lazy {
        """Wpisał\(a\):</td>\s+<th>\s+(.+?), (.+?), ([0-9]{1,2}) (.+?) ([0-9]{4}), godzina ([0-9:]+)""".toRegex()
    }


    val MOBIDZIENNIK_TIMETABLE_TOP by lazy {
        """<div class="plansc_top">.+?</div></div>""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_TIMETABLE_CELL by lazy {
        """<div class="plansc_cnt_w" style="(.+?)">.+?style="(.+?)".+?title="(.+?)".+?>\s+(.+?)\s+</div>""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_TIMETABLE_LEFT by lazy {
        """<div class="plansc_godz">.+?</div></div>""".toRegex(DOT_MATCHES_ALL)
    }


    val MOBIDZIENNIK_EVENT_CONTENT by lazy {
        """<h1>(.+?) <small>\(wpisał\(a\) (.+?) w dniu ([0-9-]{10})\).+?<strong>(.+?)</strong><br""".toRegex(DOT_MATCHES_ALL)
    }


    val VULCAN_WEB_PERMISSIONS by lazy {
        """permissions: '([A-z0-9/=+\-_|]+?)'""".toRegex()
    }
    val VULCAN_WEB_SYMBOL_VALIDATE by lazy {
        """[A-z0-9]+""".toRegex(IGNORE_CASE)
    }



    val LIBRUS_ATTACHMENT_KEY by lazy {
        """singleUseKey=([0-9A-z_]+)""".toRegex()
    }
    val LIBRUS_MESSAGE_ID by lazy {
        """/wiadomosci/[0-9]+/[0-9]+/([0-9]+?)/""".toRegex()
    }


    val LINKIFY_DATE_YMD by lazy {
        """(1\d{3}|20\d{2})[\-./](1[0-2]|0?\d)[\-./]([1-2]\d|3[0-1]|0?\d)""".toRegex()
    }
    val LINKIFY_DATE_DMY by lazy {
        """(?<![\d\-./])([1-2]\d|3[0-1]|0?\d)[\-./](1[0-2]|0?\d)(?:[\-./](1\d{3}|2?0?\d{2}))?(?![\d\-/])""".toRegex()
    }
    val LINKIFY_DATE_ABSOLUTE by lazy {
        """([1-3][0-9]|[1-9])\s(sty|lut|mar|kwi|maj|cze|lip|sie|wrz|paź|lis|gru).*?\s(1[0-9]{3}|20[0-9]{2})?""".toRegex(IGNORE_CASE)
    }
    val LINKIFY_DATE_RELATIVE by lazy {
        """za\s([0-9]+)?\s?(dni|dzień|tydzień|tygodnie)""".toRegex(IGNORE_CASE)
    }
}

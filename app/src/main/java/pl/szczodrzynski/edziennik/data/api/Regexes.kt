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
        """class="szczesliwy_numerek".*>0*([0-9]+)(?:/0*[0-9]+)*</a>""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_CLASS_CALENDAR by lazy {
        """events: (.+),$""".toRegex(RegexOption.MULTILINE)
    }

    val MOBIDZIENNIK_MESSAGE_READ_DATE by lazy {
        """czas przeczytania:.+?,\s([0-9]+)\s(.+?)\s([0-9]{4}),\sgodzina\s([0-9:]+)""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_MESSAGE_SENT_READ_DATE by lazy {
        """.+?,\s([0-9]+)\s(.+?)\s([0-9]{4}),\sgodzina\s([0-9:]+)""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_MESSAGE_ATTACHMENT by lazy {
        """href="https://.+?\.mobidziennik.pl/.+?&(?:amp;)?zalacznik=([0-9]+)"(?:.+?<small.+?\(([0-9.]+)\s(M|K|G|)B\))*""".toRegex(DOT_MATCHES_ALL)
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
    val MOBIDZIENNIK_ATTENDANCE_RANGE by lazy {
        """<span>([0-9:]+) - .+? (.+?)</span></a>""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_ATTENDANCE_LESSON by lazy {
        """<strong>(.+?)</strong>\s*<small>\s*\((.+?),\s*(.+?)\)""".toRegex(DOT_MATCHES_ALL)
    }

    val MOBIDZIENNIK_HOMEWORK_ROW by lazy {
        """class="rowRolling">(.+?</div>\s*</td>)""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_HOMEWORK_ITEM by lazy {
        """<p><b>(.+?):</b>\s*(.+?)\s*</p>""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_HOMEWORK_BODY by lazy {
        """Treść:</b>(.+?)<p><b>""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_HOMEWORK_ID by lazy {
        """zadanieFormularz\(([0-9]+),""".toRegex(DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_HOMEWORK_ATTACHMENT by lazy {
        """zalacznik(_zadania)?=([0-9]+)'.+?word-break">(.+?)</td>""".toRegex(DOT_MATCHES_ALL)
    }



    val IDZIENNIK_LOGIN_HIDDEN_FIELDS by lazy {
        """<input type="hidden".+?name="([A-z0-9_]+)?".+?value="([A-z0-9_+-/=]+)?".+?>""".toRegex(DOT_MATCHES_ALL)
    }
    val IDZIENNIK_LOGIN_ERROR by lazy {
        """id="spanErrorMessage">(.*?)</""".toRegex(DOT_MATCHES_ALL)
    }
    val IDZIENNIK_LOGIN_FIRST_ACCOUNT_NAME by lazy {
        """Imię i nazwisko:.+?">(.+?)</div>""".toRegex(DOT_MATCHES_ALL)
    }
    val IDZIENNIK_LOGIN_FIRST_IS_PARENT by lazy {
        """id="ctl00_CzyRodzic" value="([01])" />""".toRegex()
    }
    val IDZIENNIK_LOGIN_FIRST_SCHOOL_YEAR by lazy {
        """name="ctl00\${"$"}dxComboRokSzkolny".+?selected="selected".*?value="([0-9]+)">([0-9]+)/([0-9]+)<""".toRegex(DOT_MATCHES_ALL)
    }
    val IDZIENNIK_LOGIN_FIRST_STUDENT_SELECT by lazy {
        """<select.*?name="ctl00\${"$"}dxComboUczniowie".*?</select>""".toRegex(DOT_MATCHES_ALL)
    }
    val IDZIENNIK_LOGIN_FIRST_STUDENT by lazy {
        """<option.*?value="([0-9]+)"\sdata-id-ucznia="([A-z0-9]+?)".*?>(.+?)\s(.+?)\s*\((.+?),\s*(.+?)\)</option>""".toRegex(DOT_MATCHES_ALL)
    }
    val IDZIENNIK_MESSAGES_RECIPIENT_PARENT by lazy {
        """(.+?)\s\((.+)\)""".toRegex()
    }
    /*<span id="ctl00_spanSzczesliwyLos">Szczęśliwy los na dzisiaj to <b>19</b>. Los na jutro to <b>22</b></span>*/
    val IDZIENNIK_WEB_LUCKY_NUMBER by lazy {
        """dzisiaj to <b>([0-9]+)</b>""".toRegex()
    }
    val IDZIENNIK_WEB_SELECTED_REGISTER by lazy {
        """selected="selected" value="([0-9]+)" data-id-ucznia""".toRegex()
    }



    val VULCAN_SHIFT_ANNOTATION by lazy {
        """\(przeniesiona (z|na) lekcj[ię] ([0-9]+), (.+)\)""".toRegex()
    }



    val LIBRUS_ATTACHMENT_KEY by lazy {
        """singleUseKey=([0-9A-z_]+)""".toRegex()
    }



    val EDUDZIENNIK_STUDENTS_START by lazy {
        """<li><a href="/Students/([\w-_]+?)/start/">(.*?)</a>""".toRegex()
    }
    val EDUDZIENNIK_ACCOUNT_NAME_START by lazy {
        """<span id='user_dn'>(.*?)</span>""".toRegex()
    }
    val EDUDZIENNIK_SUBJECTS_START by lazy {
        """<a class="menu-course" href="/Students/[\w-_]+?/Courses/([\w-_]+)/">(.+?)</a>""".toRegex()
    }

    val EDUDZIENNIK_ATTENDANCE_ENTRIES by lazy {
        """<td id="([\d-]+?):(\d+?)".*?>(.+?)</td>""".toRegex()
    }
    val EDUDZIENNIK_ATTENDANCE_TYPES by lazy {
        """<div class="info">.*?<p>(.*?)</p>""".toRegex(DOT_MATCHES_ALL)
    }
    val EDUDZIENNIK_ATTENDANCE_TYPE by lazy {
        """\((.+?)\) (.+)""".toRegex()
    }

    val EDUDZIENNIK_ANNOUNCEMENT_DESCRIPTION by lazy {
        """<div class="desc">.*?<p>(.*?)</p>""".toRegex(DOT_MATCHES_ALL)
    }
    val EDUDZIENNIK_HOMEWORK_DESCRIPTION by lazy {
        """<div class="desc">(.*?)</div>""".toRegex(DOT_MATCHES_ALL)
    }

    val EDUDZIENNIK_SUBJECT_ID by lazy {
        """/Courses/([\w-_]+?)/""".toRegex()
    }
    val EDUDZIENNIK_GRADE_ID by lazy {
        """/Grades/([\w-_]+?)/""".toRegex()
    }
    val EDUDZIENNIK_EXAM_ID by lazy {
        """/Evaluations/([\w-_]+?)/""".toRegex()
    }
    val EDUDZIENNIK_EVENT_TYPE_ID by lazy {
        """/GradeLabels/([\w-_]+?)/""".toRegex()
    }
    val EDUDZIENNIK_ANNOUNCEMENT_ID by lazy {
        """/Announcement/([\w-_]+?)/""".toRegex()
    }
    val EDUDZIENNIK_HOMEWORK_ID by lazy {
        """/Homework/([\w-_]+?)/""".toRegex()
    }
    val EDUDZIENNIK_TEACHER_ID by lazy {
        """/Teachers/([\w-_]+?)/""".toRegex()
    }
    val EDUDZIENNIK_EVENT_ID by lazy {
        """/KlassEvent/([\w-_]+?)/""".toRegex()
    }
    val EDUDZIENNIK_NOTE_ID by lazy {
        """/RegistryNotes/([\w-_]+?)/""".toRegex()
    }

    val EDUDZIENNIK_SCHOOL_DETAIL_ID by lazy {
        """<a id="School_detail".*?/School/([\w-_]+?)/""".toRegex(DOT_MATCHES_ALL)
    }
    val EDUDZIENNIK_SCHOOL_DETAIL_NAME by lazy {
        """</li>.*?<p>(.*?)</p>.*?<li>""".toRegex(DOT_MATCHES_ALL)
    }
    val EDUDZIENNIK_CLASS_DETAIL_ID by lazy {
        """<a id="Klass_detail".*?/Klass/([\w-_]+?)/""".toRegex(DOT_MATCHES_ALL)
    }
    val EDUDZIENNIK_CLASS_DETAIL_NAME by lazy {
        """<a id="Klass_detail".*?>(.*?)</a>""".toRegex(DOT_MATCHES_ALL)
    }

    val EDUDZIENNIK_TEACHERS by lazy {
        """<div class="teacher">.*?<p>(.+?) (.+?)</p>""".toRegex(DOT_MATCHES_ALL)
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

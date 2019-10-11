/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-6.
 */

package pl.szczodrzynski.edziennik.api.v2

object Regexes {
    val MOBIDZIENNIK_GRADES_SUBJECT_NAME by lazy {
        """<div.*?>\n*\s*(.+?)\s*\n*(?:<.*?)??</div>""".toRegex(RegexOption.DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_GRADES_COLOR by lazy {
        """background-color:([#A-Fa-f0-9]+);""".toRegex(RegexOption.DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_GRADES_CATEGORY by lazy {
        """>&nbsp;(.+?):</span>""".toRegex(RegexOption.DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_GRADES_CLASS_AVERAGE by lazy {
        """Średnia ocen:.*<strong>([0-9]*\.?[0-9]*)</strong>""".toRegex(RegexOption.DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_GRADES_ADDED_DATE by lazy {
        """Wpisano:.*<strong>.+?,\s([0-9]+)\s(.+?)\s([0-9]{4}),\sgodzina\s([0-9:]+)</strong>""".toRegex(RegexOption.DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_GRADES_COUNT_TO_AVG by lazy {
        """Liczona do średniej:.*?<strong>nie<br/?></strong>""".toRegex(RegexOption.DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_GRADES_DETAILS by lazy {
        """<strong.*?>(.+?)</strong>.*?<sup>.+?</sup>.*?<small>\((.+?)\)</small>.*?<span>.*?Wartość oceny:.*?<strong>([0-9.]+)</strong>.*?Wpisał\(a\):.*?<strong>(.+?)</strong>""".toRegex(RegexOption.DOT_MATCHES_ALL)
    }

    val MOBIDZIENNIK_EVENT_TYPE by lazy {
        """\(([0-9A-ząęóżźńśłć]*?)\)$""".toRegex(RegexOption.DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_LUCKY_NUMBER by lazy {
        """class="szczesliwy_numerek".*>0*([0-9]+)(?:/0*[0-9]+)*</a>""".toRegex(RegexOption.DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_CLASS_CALENDAR by lazy {
        """events: (.+),$""".toRegex(RegexOption.MULTILINE)
    }
}

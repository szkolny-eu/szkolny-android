/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-6.
 */

package pl.szczodrzynski.edziennik.api.v2

object Regexes {
    val MOBIDZIENNIK_GRADES_SUBJECT_NAME by lazy {
        "<div.*?>\\n*\\s*(.+?)\\s*\\n*(?:<.*?)??</div>".toRegex(RegexOption.DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_EVENT_TYPE by lazy {
        "\\(([0-9A-ząęóżźńśłć]*?)\\)$".toRegex(RegexOption.DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_LUCKY_NUMBER by lazy {
        "class=\"szczesliwy_numerek\".*>0*([0-9]+)(?:/0*[0-9]+)*</a>".toRegex(RegexOption.DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_CLASS_CALENDAR by lazy {
        "events: (.+),$".toRegex(RegexOption.DOT_MATCHES_ALL)
    }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-6.
 */

package pl.szczodrzynski.edziennik.api.v2

import java.util.regex.Pattern

object Regexes {
    val MOBIDZIENNIK_GRADES_SUBJECT_NAME by lazy {
        "<div.*?>\\n*\\s*(.+?)\\s*\\n*(?:<.*?)??</div>".toRegex(RegexOption.DOT_MATCHES_ALL)
    }
    val MOBIDZIENNIK_EVENT_TYPE by lazy {
        "\\(([0-9A-ząęóżźńśłć]*?)\\)$".toRegex(RegexOption.DOT_MATCHES_ALL)
    }
}

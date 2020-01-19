/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-26.
 */

package pl.szczodrzynski.edziennik.config

import pl.szczodrzynski.edziennik.config.utils.get
import pl.szczodrzynski.edziennik.config.utils.set

class ConfigGrades(private val config: Config) {
    companion object {
        const val ORDER_BY_DATE_DESC = 0
        const val ORDER_BY_SUBJECT_ASC = 1
        const val ORDER_BY_DATE_ASC = 2
        const val ORDER_BY_SUBJECT_DESC = 3
    }

    private var mOrderBy: Int? = null
    var orderBy: Int
        get() { mOrderBy = mOrderBy ?: config.values.get("gradesOrderBy", 0); return mOrderBy ?: ORDER_BY_DATE_DESC }
        set(value) { config.set("gradesOrderBy", value); mOrderBy = value }
}
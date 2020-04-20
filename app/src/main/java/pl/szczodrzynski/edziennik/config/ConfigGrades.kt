/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-26.
 */

package pl.szczodrzynski.edziennik.config

import pl.szczodrzynski.edziennik.config.utils.get
import pl.szczodrzynski.edziennik.config.utils.set
import pl.szczodrzynski.edziennik.utils.managers.GradesManager

class ConfigGrades(private val config: Config) {
    private var mOrderBy: Int? = null
    var orderBy: Int
        get() { mOrderBy = mOrderBy ?: config.values.get("gradesOrderBy", 0); return mOrderBy ?: GradesManager.ORDER_BY_DATE_DESC }
        set(value) { config.set("gradesOrderBy", value); mOrderBy = value }
}

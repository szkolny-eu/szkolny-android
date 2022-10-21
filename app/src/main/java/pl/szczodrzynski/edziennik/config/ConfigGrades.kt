/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-26.
 */

package pl.szczodrzynski.edziennik.config

import pl.szczodrzynski.edziennik.utils.managers.GradesManager.Companion.ORDER_BY_DATE_DESC

@Suppress("RemoveExplicitTypeArguments")
class ConfigGrades(base: Config) {

    var orderBy by base.config<Int>("gradesOrderBy", ORDER_BY_DATE_DESC)
}

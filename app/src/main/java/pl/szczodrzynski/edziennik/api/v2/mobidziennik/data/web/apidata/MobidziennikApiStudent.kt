/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-6.
 */

package pl.szczodrzynski.edziennik.api.v2.mobidziennik.data.web.apidata

import pl.szczodrzynski.edziennik.api.v2.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher

class MobidziennikApiStudent(val data: DataMobidziennik, rows: List<String>) {
    init {
        for (row in rows) {
            if (row.isEmpty())
                continue
            // TODO
        }
    }
}
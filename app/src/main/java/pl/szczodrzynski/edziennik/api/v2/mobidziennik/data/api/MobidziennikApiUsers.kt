/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-6.
 */

package pl.szczodrzynski.edziennik.api.v2.mobidziennik.data.api

import pl.szczodrzynski.edziennik.api.v2.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.db.modules.teachers.Teacher
import pl.szczodrzynski.edziennik.fixWhiteSpaces

class MobidziennikApiUsers(val data: DataMobidziennik, rows: List<String>) {
    init {
        for (row in rows) {
            if (row.isEmpty())
                continue
            val cols = row.split("|")

            val id = cols[0].toLong()
            val name = cols[4].fixWhiteSpaces()
            val surname = cols[5].fixWhiteSpaces()

            data.teachersMap.put(id, "$surname $name")
            data.teacherList.put(id, Teacher(data.profileId, id, name, surname))
        }
    }
}
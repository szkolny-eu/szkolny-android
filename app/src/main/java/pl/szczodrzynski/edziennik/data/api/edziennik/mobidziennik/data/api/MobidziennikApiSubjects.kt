/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-6.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.api

import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.db.modules.subjects.Subject

class MobidziennikApiSubjects(val data: DataMobidziennik, rows: List<String>) {
    init {
        for (row in rows) {
            if (row.isEmpty())
                continue
            val cols = row.split("|")

            val id = cols[0].toLong()
            val longName = cols[1].trim()
            val shortName = cols[2].trim()

            data.subjectsMap.put(id, longName)
            data.subjectList.put(id, Subject(data.profileId, id, longName, shortName))
        }
    }
}

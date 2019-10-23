/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-23.
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.api

import pl.szczodrzynski.edziennik.DAY
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.ENDPOINT_LIBRUS_API_SUBJECTS
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.modules.subjects.Subject
import pl.szczodrzynski.edziennik.getJsonArray
import pl.szczodrzynski.edziennik.getLong
import pl.szczodrzynski.edziennik.getString

class LibrusApiSubjects(override val data: DataLibrus,
                              val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiSubjects"
    }

    init {
        apiGet(TAG, "Subjects") { json ->
            json.getJsonArray("Subjects")?.forEach { subjectEl ->
                val subject = subjectEl.asJsonObject

                val id = subject.getLong("Id") ?: return@forEach
                val longName = subject.getString("Name") ?: ""
                val shortName = subject.getString("Short") ?: ""

                data.subjectList.put(id, Subject(profileId, id, longName, shortName))
            }

            data.subjectList.put(1, Subject(profileId, 1, "Zachowanie", "zach"))

            data.setSyncNext(ENDPOINT_LIBRUS_API_SUBJECTS, 4*DAY)
            onSuccess()
        }
    }
}

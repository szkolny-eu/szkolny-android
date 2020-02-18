/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-6.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_LESSONS
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.entity.LibrusLesson

class LibrusApiLessons(override val data: DataLibrus,
                       override val lastSync: Long?,
                       val onSuccess: (endpointId: Int) -> Unit
) : LibrusApi(data, lastSync) {
    companion object {
        const val TAG = "LibrusApiLessons"
    }

    init {
        apiGet(TAG, "Lessons") { json ->
            val lessons = json.getJsonArray("Lessons")?.asJsonObjectList()

            lessons?.forEach { lesson ->
                val id = lesson.getLong("Id") ?: return@forEach
                val teacherId = lesson.getJsonObject("Teacher")?.getLong("Id") ?: return@forEach
                val subjectId = lesson.getJsonObject("Subject")?.getLong("Id") ?: return@forEach
                val teamId = lesson.getJsonObject("Class")?.getLong("Id")

                val librusLesson = LibrusLesson(
                        profileId,
                        id,
                        teacherId,
                        subjectId,
                        teamId
                )

                data.librusLessons.put(id, librusLesson)
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_LESSONS, 4*DAY)
            onSuccess(ENDPOINT_LIBRUS_API_LESSONS)
        }
    }
}

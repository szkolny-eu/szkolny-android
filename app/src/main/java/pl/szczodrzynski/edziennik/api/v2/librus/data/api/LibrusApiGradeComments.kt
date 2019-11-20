/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-20
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.api

import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.ENDPOINT_LIBRUS_API_NORMAL_GRADE_COMMENTS
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.asJsonObjectList
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.grades.GradeCategory
import pl.szczodrzynski.edziennik.getJsonArray
import pl.szczodrzynski.edziennik.getLong
import pl.szczodrzynski.edziennik.getString

class LibrusApiGradeComments(override val data: DataLibrus,
                             val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiGradeComments"
    }

    init {
        apiGet(TAG, "Grades/Comments") { json ->

            json.getJsonArray("Comments")?.asJsonObjectList()?.forEach { comment ->
                val id = comment.getLong("Id") ?: return@forEach
                val text = comment.getString("Text")

                val gradeCategoryObject = GradeCategory(
                        profileId,
                        id,
                        -1f,
                        -1,
                        text
                ).apply {
                    type = GradeCategory.TYPE_COMMENT
                }

                data.gradeCategories.put(id, gradeCategoryObject)
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_NORMAL_GRADE_COMMENTS, SYNC_ALWAYS)
            onSuccess()
        }
    }
}

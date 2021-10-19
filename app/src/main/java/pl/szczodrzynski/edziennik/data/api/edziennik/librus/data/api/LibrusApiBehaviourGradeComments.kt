/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-7
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADE_COMMENTS
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.entity.GradeCategory
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.ext.*

class LibrusApiBehaviourGradeComments(override val data: DataLibrus,
                                      override val lastSync: Long?,
                                      val onSuccess: (endpointId: Int) -> Unit
) : LibrusApi(data, lastSync) {
    companion object {
        const val TAG = "LibrusApiBehaviourGradeComments"
    }

    init {
        apiGet(TAG, "BehaviourGrades/Points/Comments") { json ->

            json.getJsonArray("Comments")?.asJsonObjectList()?.forEach { comment ->
                val id = comment.getLong("Id") ?: return@forEach
                val text = comment.getString("Text")?.fixWhiteSpaces() ?: return@forEach

                val gradeCategoryObject = GradeCategory(
                        profileId,
                        id,
                        -1f,
                        -1,
                        text
                ).apply {
                    type = GradeCategory.TYPE_BEHAVIOUR_COMMENT
                }

                data.gradeCategories.put(id, gradeCategoryObject)
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADE_COMMENTS, SYNC_ALWAYS)
            onSuccess(ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADE_COMMENTS)
        }
    }
}

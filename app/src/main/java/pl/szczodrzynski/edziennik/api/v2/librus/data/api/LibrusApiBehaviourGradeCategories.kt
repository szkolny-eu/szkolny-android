/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-3
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.api

import android.graphics.Color
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADE_CATEGORIES
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.modules.grades.GradeCategory

class LibrusApiBehaviourGradeCategories(override val data: DataLibrus,
                                        val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiBehaviourGradeCategories"
    }

    init {
        apiGet(TAG, "BehaviourGrades/Points/Categories") { json ->
            json.getJsonArray("Categories")?.asJsonObjectList()?.forEach { category ->
                val id = category.getLong("Id") ?: return@forEach
                val name = category.getString("Name") ?: ""
                val valueFrom = category.getFloat("ValueFrom") ?: 0f
                val valueTo = category.getFloat("ValueTo") ?: 0f

                val gradeCategoryObject = GradeCategory(
                        profileId,
                        id,
                        -1f,
                        Color.BLUE,
                        name
                ).apply {
                    type = GradeCategory.TYPE_BEHAVIOUR
                    setValueRange(valueFrom, valueTo)
                }

                data.gradeCategories.put(id, gradeCategoryObject)
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADE_CATEGORIES, 1 * WEEK)
            onSuccess()
        }
    }
}

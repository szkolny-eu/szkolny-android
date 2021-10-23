/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-29
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import android.graphics.Color
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_TEXT_GRADE_CATEGORIES
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.entity.GradeCategory
import pl.szczodrzynski.edziennik.ext.*

class LibrusApiTextGradeCategories(override val data: DataLibrus,
                                   override val lastSync: Long?,
                                   val onSuccess: (endpointId: Int) -> Unit
) : LibrusApi(data, lastSync) {
    companion object {
        const val TAG = "LibrusApiTextGradeCategories"
    }

    init {
        apiGet(TAG, "TextGrades/Categories") { json ->
            json.getJsonArray("Categories")?.asJsonObjectList()?.forEach { category ->
                val id = category.getLong("Id") ?: return@forEach
                val name = category.getString("Name") ?: ""
                val color = category.getJsonObject("Color")?.getInt("Id")
                        ?.let { data.getColor(it) } ?: Color.BLUE

                val gradeCategoryObject = GradeCategory(
                        profileId,
                        id,
                        -1f,
                        color,
                        name
                ).apply {
                    type = GradeCategory.TYPE_TEXT
                }

                data.gradeCategories.put(id, gradeCategoryObject)
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_TEXT_GRADE_CATEGORIES, 1 * DAY)
            onSuccess(ENDPOINT_LIBRUS_API_TEXT_GRADE_CATEGORIES)
        }
    }
}

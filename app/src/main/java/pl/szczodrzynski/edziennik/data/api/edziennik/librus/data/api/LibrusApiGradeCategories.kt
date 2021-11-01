/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-5
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import android.graphics.Color
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_NORMAL_GRADE_CATEGORIES
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.entity.GradeCategory
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.ext.*

class LibrusApiGradeCategories(override val data: DataLibrus,
                               override val lastSync: Long?,
                               val onSuccess: (endpointId: Int) -> Unit
) : LibrusApi(data, lastSync) {
    companion object {
        const val TAG = "LibrusApiGradeCategories"
    }

    init {
        apiGet(TAG, "Grades/Categories") { json ->
            json.getJsonArray("Categories")?.asJsonObjectList()?.forEach { category ->
                val id = category.getLong("Id") ?: return@forEach
                val name = category.getString("Name")?.fixWhiteSpaces() ?: ""
                val weight = when (category.getBoolean("CountToTheAverage")) {
                    true -> category.getFloat("Weight") ?: 0f
                    else -> 0f
                }
                val color = category.getJsonObject("Color")?.getInt("Id")
                        ?.let { data.getColor(it) } ?: Color.BLUE

                val gradeCategoryObject = GradeCategory(
                        profileId,
                        id,
                        weight,
                        color,
                        name
                )

                data.gradeCategories.put(id, gradeCategoryObject)
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_NORMAL_GRADE_CATEGORIES, SYNC_ALWAYS)
            onSuccess(ENDPOINT_LIBRUS_API_NORMAL_GRADE_CATEGORIES)
        }
    }
}

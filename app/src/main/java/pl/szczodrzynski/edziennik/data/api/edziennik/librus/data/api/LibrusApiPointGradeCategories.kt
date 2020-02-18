/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-29
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import android.graphics.Color
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_POINT_GRADE_CATEGORIES
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.entity.GradeCategory

class LibrusApiPointGradeCategories(override val data: DataLibrus,
                                    override val lastSync: Long?,
                                    val onSuccess: (endpointId: Int) -> Unit
) : LibrusApi(data, lastSync) {
    companion object {
        const val TAG = "LibrusApiPointGradeCategories"
    }

    init {
        apiGet(TAG, "PointGrades/Categories") { json ->
            json.getJsonArray("Categories")?.asJsonObjectList()?.forEach { category ->
                val id = category.getLong("Id") ?: return@forEach
                val name = category.getString("Name") ?: ""
                val color = category.getJsonObject("Color")?.getInt("Id")
                        ?.let { data.getColor(it) } ?: Color.BLUE
                val countToAverage = category.getBoolean("CountToTheAverage") ?: true
                val weight = if (countToAverage) category.getFloat("Weight") ?: 0f else 0f
                val valueFrom = category.getFloat("ValueFrom") ?: 0f
                val valueTo = category.getFloat("ValueTo") ?: 0f

                val gradeCategoryObject = GradeCategory(
                        profileId,
                        id,
                        weight,
                        color,
                        name
                ).apply {
                    type = GradeCategory.TYPE_POINT
                    setValueRange(valueFrom, valueTo)
                }

                data.gradeCategories.put(id, gradeCategoryObject)
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_POINT_GRADE_CATEGORIES, 1 * DAY)
            onSuccess(ENDPOINT_LIBRUS_API_POINT_GRADE_CATEGORIES)
        }
    }
}

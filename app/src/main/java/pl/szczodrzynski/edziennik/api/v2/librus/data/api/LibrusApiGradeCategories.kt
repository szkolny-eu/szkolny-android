/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-5
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.api

import android.graphics.Color
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.ENDPOINT_LIBRUS_API_NORMAL_GC
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.grades.GradeCategory

class LibrusApiGradeCategories(override val data: DataLibrus,
                               val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiGradeCategories"
    }

    init {
        apiGet(TAG, "Grades/Categories") { json ->
            json.getJsonArray("Categories")?.asJsonObjectList()?.forEach { category ->
                val id = category.getLong("Id") ?: return@forEach
                val name = category.getString("Name") ?: ""
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

            data.setSyncNext(ENDPOINT_LIBRUS_API_NORMAL_GC, SYNC_ALWAYS)
            onSuccess()
        }
    }
}

/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-29
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_DESCRIPTIVE_GRADES
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.grades.Grade
import pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_DESCRIPTIVE_TEXT
import pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_TEXT
import pl.szczodrzynski.edziennik.data.db.modules.grades.GradeCategory
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.utils.models.Date

class LibrusApiDescriptiveGrades(override val data: DataLibrus,
                                 val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiDescriptiveGrades"
    }

    init { data.profile?.also { profile ->
        apiGet(TAG, "BaseTextGrades") { json ->

            json.getJsonArray("Grades")?.asJsonObjectList()?.forEach { grade ->
                val id = grade.getLong("Id") ?: return@forEach
                val teacherId = grade.getJsonObject("AddedBy")?.getLong("Id") ?: return@forEach
                val semester = grade.getInt("Semester") ?: return@forEach
                val subjectId = grade.getJsonObject("Subject")?.getLong("Id") ?: return@forEach
                val description = grade.getString("Grade")

                val categoryId = grade.getJsonObject("Skill")?.getLong("Id")
                        ?: grade.getJsonObject("Category")?.getLong("Id")
                        ?: return@forEach
                val type = when (grade.getJsonObject("Category")) {
                    null -> TYPE_DESCRIPTIVE_TEXT
                    else -> TYPE_TEXT
                }

                val category = data.gradeCategories.singleOrNull {
                    it.categoryId == categoryId && it.type == when (type) {
                        TYPE_DESCRIPTIVE_TEXT -> GradeCategory.TYPE_DESCRIPTIVE
                        else -> GradeCategory.TYPE_NORMAL
                    }
                }

                val addedDate = Date.fromIso(grade.getString("AddDate") ?: return@forEach)

                val gradeObject = Grade(
                        profileId,
                        id,
                        category?.text ?: "",
                        category?.color ?: -1,
                        description,
                        " ",
                        0f,
                        0f,
                        semester,
                        teacherId,
                        subjectId
                ).apply {
                    this.type = type
                }

                data.gradeList.add(gradeObject)
                data.metadataList.add(Metadata(
                        profileId,
                        Metadata.TYPE_GRADE,
                        id,
                        profile.empty,
                        profile.empty,
                        addedDate
                ))
            }

            data.toRemove.addAll(listOf(
                    TYPE_DESCRIPTIVE_TEXT,
                    TYPE_TEXT
            ).map {
                DataRemoveModel.Grades.semesterWithType(profile.currentSemester, it)
            })

            data.setSyncNext(ENDPOINT_LIBRUS_API_DESCRIPTIVE_GRADES, SYNC_ALWAYS)
            onSuccess()
        }
    } ?: onSuccess() }
}

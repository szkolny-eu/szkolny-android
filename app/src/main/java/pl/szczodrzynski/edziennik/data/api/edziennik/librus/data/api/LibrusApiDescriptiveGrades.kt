/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-29
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_DESCRIPTIVE_GRADES
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_DESCRIPTIVE_TEXT
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_TEXT
import pl.szczodrzynski.edziennik.data.db.entity.GradeCategory
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.utils.models.Date

class LibrusApiDescriptiveGrades(override val data: DataLibrus,
                                 override val lastSync: Long?,
                                 val onSuccess: (endpointId: Int) -> Unit
) : LibrusApi(data, lastSync) {
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
                        profileId = profileId,
                        id = id,
                        name = " ",
                        type = type,
                        value = 0f,
                        weight = 0f,
                        color = category?.color ?: -1,
                        category = category?.text,
                        description = description,
                        comment = null,
                        semester = semester,
                        teacherId = teacherId,
                        subjectId = subjectId,
                        addedDate = addedDate,
                        code = null
                )

                data.gradeList.add(gradeObject)
                data.metadataList.add(Metadata(
                        profileId,
                        MetadataType.GRADE,
                        id,
                        profile.empty,
                        profile.empty
                ))
            }

            data.toRemove.addAll(listOf(
                    TYPE_DESCRIPTIVE_TEXT,
                    TYPE_TEXT
            ).map {
                DataRemoveModel.Grades.semesterWithType(profile.currentSemester, it)
            })

            data.setSyncNext(ENDPOINT_LIBRUS_API_DESCRIPTIVE_GRADES, SYNC_ALWAYS)
            onSuccess(ENDPOINT_LIBRUS_API_DESCRIPTIVE_GRADES)
        }
    } ?: onSuccess(ENDPOINT_LIBRUS_API_DESCRIPTIVE_GRADES) }
}

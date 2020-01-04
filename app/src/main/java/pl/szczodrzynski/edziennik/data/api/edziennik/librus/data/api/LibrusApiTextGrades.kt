/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-29
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_TEXT_GRADES
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.grades.Grade
import pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_DESCRIPTIVE
import pl.szczodrzynski.edziennik.data.db.modules.grades.GradeCategory
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.utils.models.Date

class LibrusApiTextGrades(override val data: DataLibrus,
                          val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiTextGrades"
    }

    init { data.profile?.also { profile ->
        apiGet(TAG, "DescriptiveGrades") { json ->

            json.getJsonArray("Grades")?.asJsonObjectList()?.forEach { grade ->
                val id = grade.getLong("Id") ?: return@forEach
                val teacherId = grade.getJsonObject("AddedBy")?.getLong("Id") ?: return@forEach
                val semester = grade.getInt("Semester") ?: return@forEach
                val subjectId = grade.getJsonObject("Subject")?.getLong("Id") ?: return@forEach

                val map = grade.getString("Map")
                val realValue = grade.getString("RealGradeValue")

                val name = map ?: realValue ?: return@forEach
                val description = if (map != null && map != realValue) realValue ?: "" else ""

                val categoryId = grade.getJsonObject("Skill")?.getLong("Id") ?: return@forEach

                val category = data.gradeCategories.singleOrNull {
                    it.categoryId == categoryId && it.type == GradeCategory.TYPE_DESCRIPTIVE
                }

                val addedDate = Date.fromIso(grade.getString("AddDate") ?: return@forEach)

                val gradeObject = Grade(
                        profileId,
                        id,
                        category?.text ?: "",
                        category?.color ?: -1,
                        description,
                        name,
                        0f,
                        0f,
                        semester,
                        teacherId,
                        subjectId
                ).apply {
                    type = TYPE_DESCRIPTIVE
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

            data.toRemove.add(DataRemoveModel.Grades.semesterWithType(profile.currentSemester, TYPE_DESCRIPTIVE))

            data.setSyncNext(ENDPOINT_LIBRUS_API_TEXT_GRADES, SYNC_ALWAYS)
            onSuccess()
        }
    } ?: onSuccess() }
}

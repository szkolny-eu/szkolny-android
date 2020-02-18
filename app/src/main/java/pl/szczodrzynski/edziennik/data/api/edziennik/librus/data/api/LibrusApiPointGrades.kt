/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-29
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_POINT_GRADES
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.entity.Grade.TYPE_POINT_AVG
import pl.szczodrzynski.edziennik.data.db.entity.GradeCategory
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.utils.models.Date

class LibrusApiPointGrades(override val data: DataLibrus,
                           override val lastSync: Long?,
                           val onSuccess: (endpointId: Int) -> Unit
) : LibrusApi(data, lastSync) {
    companion object {
        const val TAG = "LibrusApiPointGrades"
    }

    init { data.profile?.also { profile ->
        apiGet(TAG, "PointGrades") { json ->

            json.getJsonArray("Grades")?.asJsonObjectList()?.forEach { grade ->
                val id = grade.getLong("Id") ?: return@forEach
                val teacherId = grade.getJsonObject("AddedBy")?.getLong("Id") ?: return@forEach
                val semester = grade.getInt("Semester") ?: return@forEach
                val subjectId = grade.getJsonObject("Subject")?.getLong("Id") ?: return@forEach
                val name = grade.getString("Grade") ?: return@forEach
                val value = grade.getFloat("GradeValue") ?: 0f

                val categoryId = grade.getJsonObject("Category")?.getLong("Id") ?: return@forEach

                val category = data.gradeCategories.singleOrNull {
                    it.categoryId == categoryId && it.type == GradeCategory.TYPE_POINT
                }

                val addedDate = Date.fromIso(grade.getString("AddDate") ?: return@forEach)

                val gradeObject = Grade(
                        profileId,
                        id,
                        category?.text ?: "",
                        category?.color ?: -1,
                        "",
                        name,
                        value,
                        category?.weight ?: 0f,
                        semester,
                        teacherId,
                        subjectId
                ).apply {
                    type = TYPE_POINT_AVG
                    valueMax = category?.valueTo ?: 0f
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

            data.toRemove.add(DataRemoveModel.Grades.semesterWithType(profile.currentSemester, TYPE_POINT_AVG))

            data.setSyncNext(ENDPOINT_LIBRUS_API_POINT_GRADES, SYNC_ALWAYS)
            onSuccess(ENDPOINT_LIBRUS_API_POINT_GRADES)
        }
    } ?: onSuccess(ENDPOINT_LIBRUS_API_POINT_GRADES) }
}

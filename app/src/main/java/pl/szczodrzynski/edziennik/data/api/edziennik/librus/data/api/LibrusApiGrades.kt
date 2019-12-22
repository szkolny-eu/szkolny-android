package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_NORMAL_GRADES
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.grades.Grade
import pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.*
import pl.szczodrzynski.edziennik.data.db.modules.grades.GradeCategory
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.edziennik.utils.models.Date

class LibrusApiGrades(override val data: DataLibrus,
                      val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiGrades"
    }

    init { data.profile?.also { profile ->
        apiGet(TAG, "Grades") { json ->
            val grades = json.getJsonArray("Grades").asJsonObjectList()

            grades?.forEach { grade ->
                val id = grade.getLong("Id") ?: return@forEach
                val categoryId = grade.getJsonObject("Category")?.getLong("Id") ?: -1
                val name = grade.getString("Grade") ?: ""
                val semester = grade.getInt("Semester") ?: return@forEach
                val teacherId = grade.getJsonObject("AddedBy")?.getLong("Id") ?: -1
                val subjectId = grade.getJsonObject("Subject")?.getLong("Id") ?: -1
                val addedDate = Date.fromIso(grade.getString("AddDate"))

                val category = data.gradeCategories.singleOrNull { it.categoryId == categoryId }
                val categoryName = category?.text ?: ""
                val color = category?.color ?: -1
                var weight = category?.weight ?: 0f
                val value = Utils.getGradeValue(name)


                if (name == "-" || name == "+"
                        || name.equals("np", ignoreCase = true)
                        || name.equals("bz", ignoreCase = true)) {
                    weight = 0f
                }

                val description = grade.getJsonArray("Comments")?.asJsonObjectList()?.let { comments ->
                    if (comments.isNotEmpty()) {
                        data.gradeCategories.singleOrNull {
                            it.type == GradeCategory.TYPE_NORMAL_COMMENT
                                    && it.categoryId == comments[0].asJsonObject.getLong("Id")
                        }?.text
                    } else null
                } ?: ""

                val gradeObject = Grade(
                        profileId,
                        id,
                        categoryName,
                        color,
                        description,
                        name,
                        value,
                        weight,
                        semester,
                        teacherId,
                        subjectId
                )

                when {
                    grade.getBoolean("IsConstituent") ?: false ->
                        gradeObject.type = TYPE_NORMAL
                    grade.getBoolean("IsSemester") ?: false -> // semester final
                        gradeObject.type = if (gradeObject.semester == 1) TYPE_SEMESTER1_FINAL else TYPE_SEMESTER2_FINAL
                    grade.getBoolean("IsSemesterProposition") ?: false -> // semester proposed
                        gradeObject.type = if (gradeObject.semester == 1) TYPE_SEMESTER1_PROPOSED else TYPE_SEMESTER2_PROPOSED
                    grade.getBoolean("IsFinal") ?: false -> // year final
                        gradeObject.type = TYPE_YEAR_FINAL
                    grade.getBoolean("IsFinalProposition") ?: false -> // year final
                        gradeObject.type = TYPE_YEAR_PROPOSED
                }

                grade.getJsonObject("Improvement")?.also {
                    val historicalId = it.getLong("Id")
                    data.gradeList.firstOrNull { grade -> grade.id == historicalId }?.also { grade ->
                        grade.parentId = gradeObject.id
                        if (grade.name == "nb") grade.weight = 0f
                    }
                    gradeObject.isImprovement = true
                }

                data.gradeList.add(gradeObject)
                data.metadataList.add(
                        Metadata(
                                profileId,
                                Metadata.TYPE_GRADE,
                                id,
                                profile.empty,
                                profile.empty,
                                addedDate
                        ))
            }

            data.toRemove.addAll(listOf(
                    TYPE_NORMAL,
                    TYPE_SEMESTER1_FINAL,
                    TYPE_SEMESTER2_FINAL,
                    TYPE_SEMESTER1_PROPOSED,
                    TYPE_SEMESTER2_PROPOSED,
                    TYPE_YEAR_FINAL,
                    TYPE_YEAR_PROPOSED
            ).map {
                DataRemoveModel.Grades.semesterWithType(profile.currentSemester, it)
            })
            data.setSyncNext(ENDPOINT_LIBRUS_API_NORMAL_GRADES, SYNC_ALWAYS)
            onSuccess()
        }
    } ?: onSuccess() }
}

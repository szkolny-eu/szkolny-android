package pl.szczodrzynski.edziennik.api.v2.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.v2.librus.ENDPOINT_LIBRUS_API_NORMAL_GRADES
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.grades.Grade
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.edziennik.utils.models.Date


class LibrusApiGrades(override val data: DataLibrus,
                      val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiGrades"
    }

    init {
        apiGet(TAG, "Grades") { json ->
            val grades = json.getJsonArray("Grades")

            grades?.forEach { gradeEl ->
                val grade = gradeEl.asJsonObject

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

                val gradeObject = Grade(
                        profileId,
                        id,
                        categoryName,
                        color,
                        "",
                        name,
                        value,
                        weight,
                        semester,
                        teacherId,
                        subjectId
                )

                when {
                    grade.getBoolean("IsConstituent") ?: false ->
                        gradeObject.type = Grade.TYPE_NORMAL
                    grade.getBoolean("IsSemester") ?: false -> // semester final
                        gradeObject.type = if (gradeObject.semester == 1) Grade.TYPE_SEMESTER1_FINAL else Grade.TYPE_SEMESTER2_FINAL
                    grade.getBoolean("IsSemesterProposition") ?: false -> // semester proposed
                        gradeObject.type = if (gradeObject.semester == 1) Grade.TYPE_SEMESTER1_PROPOSED else Grade.TYPE_SEMESTER2_PROPOSED
                    grade.getBoolean("IsFinal") ?: false -> // year final
                        gradeObject.type = Grade.TYPE_YEAR_FINAL
                    grade.getBoolean("IsFinalProposition") ?: false -> // year final
                        gradeObject.type = Grade.TYPE_YEAR_PROPOSED
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
                                profile?.empty ?: false,
                                profile?.empty ?: false,
                                addedDate
                        ))
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_NORMAL_GRADES, SYNC_ALWAYS)
            onSuccess()
        }
    }
}

/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-3
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADES
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_POINT_SUM
import pl.szczodrzynski.edziennik.data.db.entity.GradeCategory
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.utils.models.Date
import java.text.DecimalFormat

class LibrusApiBehaviourGrades(override val data: DataLibrus,
                               override val lastSync: Long?,
                               val onSuccess: (endpointId: Int) -> Unit
) : LibrusApi(data, lastSync) {
    companion object {
        const val TAG = "LibrusApiBehaviourGrades"
    }

    private val nameFormat by lazy { DecimalFormat("#.##") }

    private val types by lazy {
        mapOf(
                1 to ("wz" to "wzorowe"),
                2 to ("bdb" to "bardzo dobre"),
                3 to ("db" to "dobre"),
                4 to ("popr" to "poprawne"),
                5 to ("ndp" to "nieodpowiednie"),
                6 to ("ng" to "naganne")
        )
    }

    init { data.profile?.also { profile ->
        apiGet(TAG, "BehaviourGrades/Points") { json ->

            if (data.startPointsSemester1 > 0) {
                val semester1StartGradeObject = Grade(
                        profileId = profileId,
                        id = -101,
                        name = nameFormat.format(data.startPointsSemester1),
                        type = TYPE_POINT_SUM,
                        value = data.startPointsSemester1.toFloat(),
                        weight = 0f,
                        color = 0xffbdbdbd.toInt(),
                        category = data.app.getString(R.string.grade_start_points),
                        description = data.app.getString(R.string.grade_start_points_format, 1),
                        comment = null,
                        semester = 1,
                        teacherId = -1,
                        subjectId = 1,
                        addedDate = profile.getSemesterStart(1).inMillis
                )

                data.gradeList.add(semester1StartGradeObject)
                data.metadataList.add(Metadata(
                        profileId,
                        MetadataType.GRADE,
                        semester1StartGradeObject.id,
                        true,
                        true
                ))
            }

            if (data.startPointsSemester2 > 0) {
                val semester2StartGradeObject = Grade(
                        profileId = profileId,
                        id = -102,
                        name = nameFormat.format(data.startPointsSemester2),
                        type = TYPE_POINT_SUM,
                        value = data.startPointsSemester2.toFloat(),
                        weight = -1f,
                        color = 0xffbdbdbd.toInt(),
                        category = data.app.getString(R.string.grade_start_points),
                        description = data.app.getString(R.string.grade_start_points_format, 2),
                        comment = null,
                        semester = 2,
                        teacherId = -1,
                        subjectId = 1,
                        addedDate = profile.getSemesterStart(2).inMillis
                )

                data.gradeList.add(semester2StartGradeObject)
                data.metadataList.add(Metadata(
                        profileId,
                        MetadataType.GRADE,
                        semester2StartGradeObject.id,
                        true,
                        true
                ))
            }

            json.getJsonArray("Grades")?.asJsonObjectList()?.forEach { grade ->
                val id = grade.getLong("Id") ?: return@forEach
                val value = grade.getFloat("Value")
                val shortName = grade.getString("ShortName")
                val semester = grade.getInt("Semester") ?: profile.currentSemester
                val teacherId = grade.getJsonObject("AddedBy")?.getLong("Id") ?: -1
                val addedDate = grade.getString("AddDate")?.let { Date.fromIso(it) }
                        ?: System.currentTimeMillis()

                val text = grade.getString("Text")
                val type = grade.getJsonObject("BehaviourGrade")?.getInt("Id")?.let { types[it] }

                val name = when {
                    type != null -> type.first
                    value != null -> (if (value > 0) "+" else "") + nameFormat.format(value)
                    shortName != null -> shortName
                    else -> return@forEach
                }

                val color = data.getColor(when {
                    value == null || value == 0f -> 12
                    value > 0 -> 16
                    value < 0 -> 26
                    else -> 12
                })

                val categoryId = grade.getJsonObject("Category")?.getLong("Id") ?: -1
                val category = data.gradeCategories.singleOrNull {
                    it.categoryId == categoryId && it.type == GradeCategory.TYPE_BEHAVIOUR
                }

                val categoryName = category?.text ?: ""

                val comments = grade.getJsonArray("Comments")
                        ?.asJsonObjectList()
                        ?.mapNotNull { comment ->
                            val cId = comment.getLong("Id") ?: return@mapNotNull null
                            data.gradeCategories[cId]?.text
                        } ?: listOf()

                val description = listOfNotNull(type?.second) + comments

                val valueFrom = value ?: category?.valueFrom ?: 0f
                val valueTo = category?.valueTo ?: 0f

                val gradeObject = Grade(
                        profileId = profileId,
                        id = id,
                        name = name,
                        type = TYPE_POINT_SUM,
                        value = valueFrom,
                        weight = -1f,
                        color = color,
                        category = categoryName,
                        description = text ?: description.join(" - "),
                        comment = if (text != null) description.join(" - ") else null,
                        semester = semester,
                        teacherId = teacherId,
                        subjectId = 1,
                        addedDate = addedDate
                ).apply {
                    valueMax = valueTo
                }

                data.gradeList.add(gradeObject)
                data.metadataList.add(Metadata(
                        profileId,
                        MetadataType.GRADE,
                        id,
                        profile.empty,
                        profile.empty
                ))
            }

            data.toRemove.add(DataRemoveModel.Grades.semesterWithType(profile.currentSemester, Grade.TYPE_POINT_SUM))
            data.setSyncNext(ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADES, SYNC_ALWAYS)
            onSuccess(ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADES)
        }
    } ?: onSuccess(ENDPOINT_LIBRUS_API_BEHAVIOUR_GRADES) }
}

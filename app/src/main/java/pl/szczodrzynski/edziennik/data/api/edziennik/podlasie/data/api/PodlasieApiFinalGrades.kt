/*
 * Copyright (c) Kacper Ziubryniewicz 2020-5-13
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.data.api

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.DataPodlasie
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_SEMESTER1_FINAL
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_SEMESTER1_PROPOSED
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_SEMESTER2_FINAL
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_SEMESTER2_PROPOSED
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_YEAR_FINAL
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_YEAR_PROPOSED
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.enums.MetadataType
import pl.szczodrzynski.edziennik.ext.getLong
import pl.szczodrzynski.edziennik.ext.getSemesterStart
import pl.szczodrzynski.edziennik.ext.getString

class PodlasieApiFinalGrades(val data: DataPodlasie, val rows: List<JsonObject>) {
    init { data.profile?.also { profile ->
        rows.forEach { grade ->
            val id = grade.getLong("ExternalId") ?: return@forEach
            val mark = grade.getString("Mark") ?: return@forEach
            val proposedMark = grade.getString("ProposedMark") ?: "0"
            val name = data.app.gradesManager.getGradeNumberName(mark)
            val value = data.app.gradesManager.getGradeValue(name)
            val semester = grade.getString("TermShortcut")?.length ?: return@forEach

            val typeName = grade.getString("Type") ?: return@forEach
            val type = when (typeName) {
                "S" -> if (semester == 1) TYPE_SEMESTER1_FINAL else TYPE_SEMESTER2_FINAL
                "Y", "R" -> TYPE_YEAR_FINAL
                else -> return@forEach
            }

            val subjectName = grade.getString("SchoolSubject") ?: return@forEach
            val subject = data.getSubject(null, subjectName)

            val addedDate = if (profile.empty) profile.getSemesterStart(semester).inMillis
            else System.currentTimeMillis()

            val gradeObject = Grade(
                    profileId = data.profileId,
                    id = id,
                    name = name,
                    type = type,
                    value = value,
                    weight = 0f,
                    color = -1,
                    category = null,
                    description = null,
                    comment = null,
                    semester = semester,
                    teacherId = -1,
                    subjectId = subject.id,
                    addedDate = addedDate
            )

            data.gradeList.add(gradeObject)
            data.metadataList.add(
                    Metadata(
                            data.profileId,
                            MetadataType.GRADE,
                            id,
                            profile.empty,
                            profile.empty
                    ))

            if (proposedMark != "0") {
                val proposedName = data.app.gradesManager.getGradeNumberName(proposedMark)
                val proposedValue = data.app.gradesManager.getGradeValue(proposedName)

                val proposedType = when (typeName) {
                    "S" -> if (semester == 1) TYPE_SEMESTER1_PROPOSED else TYPE_SEMESTER2_PROPOSED
                    "Y", "R" -> TYPE_YEAR_PROPOSED
                    else -> return@forEach
                }

                val proposedGradeObject = Grade(
                        profileId = data.profileId,
                        id = id * (-1),
                        name = proposedName,
                        type = proposedType,
                        value = proposedValue,
                        weight = 0f,
                        color = -1,
                        category = null,
                        description = null,
                        comment = null,
                        semester = semester,
                        teacherId = -1,
                        subjectId = subject.id,
                        addedDate = addedDate
                )

                data.gradeList.add(proposedGradeObject)
                data.metadataList.add(
                        Metadata(
                                data.profileId,
                                MetadataType.GRADE,
                                proposedGradeObject.id,
                                profile.empty,
                                profile.empty
                        ))
            }
        }

        data.toRemove.addAll(listOf(
                TYPE_SEMESTER1_FINAL,
                TYPE_SEMESTER1_PROPOSED,
                TYPE_SEMESTER2_FINAL,
                TYPE_SEMESTER2_PROPOSED,
                TYPE_YEAR_FINAL,
                TYPE_YEAR_PROPOSED
        ).map {
            DataRemoveModel.Grades.allWithType(it)
        })
    }}
}

/*
 * Copyright (c) Kacper Ziubryniewicz 2020-5-13
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.data.api

import android.graphics.Color
import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.DataPodlasie
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.ext.getFloat
import pl.szczodrzynski.edziennik.ext.getInt
import pl.szczodrzynski.edziennik.ext.getLong
import pl.szczodrzynski.edziennik.ext.getString
import pl.szczodrzynski.edziennik.utils.models.Date

class PodlasieApiGrades(val data: DataPodlasie, val rows: List<JsonObject>) {
    init {
        rows.forEach { grade ->
            val id = grade.getLong("ExternalId") ?: return@forEach
            val name = grade.getString("Mark") ?: return@forEach
            val value = data.app.gradesManager.getGradeValue(name)
            val weight = grade.getFloat("Weight") ?: 0f
            val includeToAverage = grade.getInt("IncludeToAverage") != 0
            val color = grade.getString("Color")?.let { Color.parseColor(it) } ?: -1
            val category = grade.getString("Category") ?: ""
            val comment = grade.getString("Comment") ?: ""
            val semester = grade.getString("TermShortcut")?.length ?: data.currentSemester

            val teacherFirstName = grade.getString("TeacherFirstName") ?: return@forEach
            val teacherLastName = grade.getString("TeacherLastName") ?: return@forEach
            val teacher = data.getTeacher(teacherFirstName, teacherLastName)

            val subjectName = grade.getString("SchoolSubject") ?: return@forEach
            val subject = data.getSubject(null, subjectName)

            val addedDate = grade.getString("ReceivedDate")?.let { Date.fromY_m_d(it).inMillis }
                    ?: System.currentTimeMillis()

            val gradeObject = Grade(
                    profileId = data.profileId,
                    id = id,
                    name = name,
                    type = Grade.TYPE_NORMAL,
                    value = value,
                    weight = if (includeToAverage) weight else 0f,
                    color = color,
                    category = category,
                    description = null,
                    comment = comment,
                    semester = semester,
                    teacherId = teacher.id,
                    subjectId = subject.id,
                    addedDate = addedDate
            )

            data.gradeList.add(gradeObject)
            data.metadataList.add(
                    Metadata(
                            data.profileId,
                            MetadataType.GRADE,
                            id,
                            data.profile?.empty ?: false,
                            data.profile?.empty ?: false
                    ))
        }

        data.toRemove.add(DataRemoveModel.Grades.allWithType(Grade.TYPE_NORMAL))
    }
}

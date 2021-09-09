/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-25
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.web

import android.graphics.Color
import org.jsoup.Jsoup
import pl.szczodrzynski.edziennik.colorFromCssName
import pl.szczodrzynski.edziennik.crc32
import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.ENDPOINT_EDUDZIENNIK_WEB_GRADES
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.EdudziennikWeb
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_NORMAL
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_POINT_SUM
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_SEMESTER1_FINAL
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_SEMESTER1_PROPOSED
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_SEMESTER2_FINAL
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_SEMESTER2_PROPOSED
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.get
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.edziennik.utils.models.Date

class EdudziennikWebGrades(override val data: DataEdudziennik,
                           override val lastSync: Long?,
                           val onSuccess: (endpointId: Int) -> Unit
) : EdudziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "EdudziennikWebGrades"
    }

    private var requestSemester: Int? = null

    init {
        if (profile?.empty == true && data.currentSemester == 2) requestSemester = 1
        getGrades()
    }

    private fun getGrades() { data.profile?.also { profile ->
        webGet(TAG, data.studentEndpoint + "start", semester = requestSemester) { text ->
            val semester = requestSemester ?: data.currentSemester

            val doc = Jsoup.parse(text)
            val subjects = doc.select("#student_grades tbody").firstOrNull()?.children()

            subjects?.forEach { subjectElement ->
                if (subjectElement.id().isBlank()) return@forEach

                val subjectId = subjectElement.id().trim()
                val subjectName = subjectElement.child(0).text().trim()
                val subject = data.getSubject(subjectId.crc32(), subjectName)

                val gradeType = when {
                    subjectElement.select("#sum").text().isNotBlank() -> TYPE_POINT_SUM
                    else -> TYPE_NORMAL
                }

                val gradeCountToAverage = subjectElement.select("#avg").text().isNotBlank()

                val grades = subjectElement.select(".grade[data-edited]")
                val gradesInfo = subjectElement.select(".grade-tip")

                val gradeValues = if (grades.isNotEmpty()) {
                    subjects.select(".avg-$subjectId .grade-tip > p").first()
                            .text().split('+').map {
                                val split = it.split('*')
                                val value = split[1].trim().toFloatOrNull()
                                val weight = value?.let { split[0].trim().toFloatOrNull() } ?: 0f

                                Pair(value ?: 0f, weight)
                            }
                } else emptyList()

                grades.forEachIndexed { index, gradeElement ->
                    val id = Regexes.EDUDZIENNIK_GRADE_ID.find(gradeElement.attr("href"))?.get(1)?.crc32()
                            ?: return@forEachIndexed
                    val (value, weight) = gradeValues[index]
                    val name = gradeElement.text().trim().let {
                        if (it.contains(',') || it.contains('.')) {
                            val replaced = it.replace(',', '.')
                            val float = replaced.toFloatOrNull()

                            if (float != null && float % 1 == 0f) float.toInt().toString()
                            else it
                        } else it
                    }

                    val info = gradesInfo[index]
                    val fullName = info.child(0).text().trim()
                    val columnName = info.child(4).text().trim()
                    val comment = info.ownText()

                    val description = columnName + if (comment.isNotBlank()) " - $comment" else null

                    val teacherName = info.child(1).text()
                    val teacher = data.getTeacherByLastFirst(teacherName)

                    val addedDate = info.child(2).text().split(' ').let {
                        val day = it[0].toInt()
                        val month = Utils.monthFromName(it[1])
                        val year = it[2].toInt()

                        Date(year, month, day).inMillis
                    }

                    val color = Regexes.STYLE_CSS_COLOR.find(gradeElement.attr("style"))?.get(1)?.let {
                        if (it.startsWith('#')) Color.parseColor(it)
                        else colorFromCssName(it)
                    } ?: -1

                    val gradeObject = Grade(
                            profileId = profileId,
                            id = id,
                            name = name,
                            type = gradeType,
                            value = value,
                            weight = if (gradeCountToAverage) weight else 0f,
                            color = color,
                            category = fullName,
                            description = description,
                            comment = null,
                            semester = semester,
                            teacherId = teacher.id,
                            subjectId = subject.id,
                            addedDate = addedDate
                    )

                    data.gradeList.add(gradeObject)
                    data.metadataList.add(Metadata(
                            profileId,
                            Metadata.TYPE_GRADE,
                            id,
                            profile.empty,
                            profile.empty
                    ))
                }

                val proposed = subjectElement.select(".proposal").firstOrNull()?.text()?.trim()

                if (proposed != null && proposed.isNotBlank()) {
                    val proposedGradeObject = Grade(
                            profileId = profileId,
                            id = (-1 * subject.id) - 1,
                            name = proposed,
                            type = when (semester) {
                                1 -> TYPE_SEMESTER1_PROPOSED
                                else -> TYPE_SEMESTER2_PROPOSED
                            },
                            value = proposed.toFloatOrNull() ?: 0f,
                            weight = 0f,
                            color = -1,
                            category = null,
                            description = null,
                            comment = null,
                            semester = semester,
                            teacherId = -1,
                            subjectId = subject.id
                    )

                    data.gradeList.add(proposedGradeObject)
                    data.metadataList.add(Metadata(
                            profileId,
                            Metadata.TYPE_GRADE,
                            proposedGradeObject.id,
                            profile.empty,
                            profile.empty
                    ))
                }

                val final = subjectElement.select(".final").firstOrNull()?.text()?.trim()

                if (final != null && final.isNotBlank()) {
                    val finalGradeObject = Grade(
                            profileId = profileId,
                            id = (-1 * subject.id) - 2,
                            name = final,
                            type = when (semester) {
                                1 -> TYPE_SEMESTER1_FINAL
                                else -> TYPE_SEMESTER2_FINAL
                            },
                            value = final.toFloatOrNull() ?: 0f,
                            weight = 0f,
                            color = -1,
                            category = null,
                            description = null,
                            comment = null,
                            semester = semester,
                            teacherId = -1,
                            subjectId = subject.id
                    )

                    data.gradeList.add(finalGradeObject)
                    data.metadataList.add(Metadata(
                            data.profileId,
                            Metadata.TYPE_GRADE,
                            finalGradeObject.id,
                            profile.empty,
                            profile.empty
                    ))
                }
            }

            if (!subjects.isNullOrEmpty()) {
                data.toRemove.addAll(listOf(
                        TYPE_NORMAL,
                        TYPE_POINT_SUM,
                        TYPE_SEMESTER1_PROPOSED,
                        TYPE_SEMESTER2_PROPOSED,
                        TYPE_SEMESTER1_FINAL,
                        TYPE_SEMESTER2_FINAL
                ).map {
                    DataRemoveModel.Grades.semesterWithType(semester, it)
                })
            }

            if (profile.empty && requestSemester == 1 && data.currentSemester == 2) {
                requestSemester = null
                getGrades()
            } else {
                data.setSyncNext(ENDPOINT_EDUDZIENNIK_WEB_GRADES, SYNC_ALWAYS)
                onSuccess(ENDPOINT_EDUDZIENNIK_WEB_GRADES)
            }
        }
    } ?: onSuccess(ENDPOINT_EDUDZIENNIK_WEB_GRADES) }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2025-1-31.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.usos.data.api

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.data.api.ERROR_USOS_API_INCOMPLETE_RESPONSE
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.DataUsos
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.ENDPOINT_USOS_API_EXAM_REPORTS
import pl.szczodrzynski.edziennik.data.api.edziennik.usos.data.UsosApi
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_NORMAL
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.ext.getBoolean
import pl.szczodrzynski.edziennik.ext.getInt
import pl.szczodrzynski.edziennik.ext.getJsonArray
import pl.szczodrzynski.edziennik.ext.getJsonObject
import pl.szczodrzynski.edziennik.ext.getLong
import pl.szczodrzynski.edziennik.ext.getString
import pl.szczodrzynski.edziennik.ext.join
import pl.szczodrzynski.edziennik.utils.models.Date

class UsosApiExamReports(
    override val data: DataUsos,
    override val lastSync: Long?,
    val onSuccess: (endpointId: Int) -> Unit,
) : UsosApi(data, lastSync) {
    companion object {
        const val TAG = "UsosApiExamReports"
    }

    private val missingTermNames = mutableSetOf<String>()

    init {
        apiRequest<JsonObject>(
            tag = TAG,
            service = "examrep/user2",
            fields = listOf(
                "type_description",
                "course_unit" to listOf("id", "course_name"),
                "sessions" to listOf(
                    "description",
                    "issuer_grades" to listOf(
                        "exam_id",
                        "exam_session_number",
                        "value_symbol",
                        // "value_description",
                        "passes",
                        "counts_into_average",
                        "date_modified",
                        "modification_author",
                        "comment",
                    ),
                ),
            ),
            responseType = ResponseType.OBJECT,
        ) { json, response ->
            if (!processResponse(json)) {
                data.error(TAG, ERROR_USOS_API_INCOMPLETE_RESPONSE, response)
                return@apiRequest
            }

            data.toRemove.add(DataRemoveModel.Grades.all())
            data.setSyncNext(ENDPOINT_USOS_API_EXAM_REPORTS, SYNC_ALWAYS)

            if (missingTermNames.isEmpty())
                onSuccess(ENDPOINT_USOS_API_EXAM_REPORTS)
            else
                UsosApiTerms(data, lastSync, onSuccess, missingTermNames)
        }
    }

    private fun processResponse(json: JsonObject): Boolean {
        for ((termId, courseEditionEl) in json.entrySet()) {
            if (!courseEditionEl.isJsonObject)
                continue
            for ((courseId, examReportsEl) in courseEditionEl.asJsonObject.entrySet()) {
                if (!examReportsEl.isJsonArray)
                    continue
                for (examReportEl in examReportsEl.asJsonArray) {
                    if (!examReportEl.isJsonObject)
                        continue
                    val examReport = examReportEl.asJsonObject
                    processExamReport(termId, courseId, examReport)
                }
            }
        }
        return true
    }

    private fun processExamReport(termId: String, courseId: String, examReport: JsonObject) {
        val typeDescription = examReport.getLangString("type_description")
        val courseUnit = examReport.getJsonObject("course_unit")
            ?: return
        val courseUnitId = courseUnit.getString("id")?.toLongOrNull()
            ?: return
        val courseName = courseUnit.getLangString("course_name")
            ?: return
        val sessions = examReport.getJsonArray("sessions")
            ?: return

        for (sessionEl in sessions) {
            if (!sessionEl.isJsonObject)
                continue
            val session = sessionEl.asJsonObject

            val sessionDescription = session.getLangString("description")
            val issuerGrade = session.getJsonObject("issuer_grades") ?: continue

            val examId = issuerGrade.getInt("exam_id") ?: continue
            val sessionNumber = issuerGrade.getInt("exam_session_number") ?: continue
            val valueSymbol = issuerGrade.getString("value_symbol") ?: continue
            val passes = issuerGrade.getBoolean("passes")
            val countsIntoAverage = issuerGrade.getString("counts_into_average") ?: "T"
            val dateModified = issuerGrade.getString("date_modified")
            val modificationAuthorId = issuerGrade.getJsonObject("modification_author")
                ?.getLong("id") ?: -1L
            val comment = issuerGrade.getString("comment")

            val gradeCategory = data.gradeCategories[courseUnitId]
            val classType = gradeCategory?.columns?.get(0)
            val value = valueSymbol.toFloatOrNull() ?: 0.0f

            if (termId !in data.termNames) {
                missingTermNames.add(termId)
            }

            val gradeObject = Grade(
                profileId = profileId,
                id = examId * 10L + sessionNumber,
                name = valueSymbol,
                type = TYPE_NORMAL,
                value = value,
                weight = if (countsIntoAverage == "T") gradeCategory?.weight ?: 0.0f else 0.0f,
                color = (if (passes == true) 0xFF465FB3 else 0xFFB71C1C).toInt(),
                category = "$termId$$typeDescription",
                description = listOfNotNull(classType, sessionDescription).join(" - "),
                comment = comment,
                semester = 1,
                teacherId = modificationAuthorId,
                subjectId = data.getSubject(
                    id = null,
                    name = courseName,
                    shortName = courseId,
                ).id,
                addedDate = Date.fromIso(dateModified),
            )

            if (sessionNumber > 1) {
                val origId = examId * 10L + sessionNumber - 1
                val grades = data.gradeList.filter { it.id == origId }
                val improvedGrade = grades.firstOrNull()
                improvedGrade?.parentId = gradeObject.id
                improvedGrade?.weight = 0.0f
                gradeObject.isImprovement = true
            }

            data.gradeList.add(gradeObject)
            data.metadataList.add(
                Metadata(
                    profileId,
                    MetadataType.GRADE,
                    gradeObject.id,
                    profile?.empty ?: false,
                    profile?.empty ?: false,
                )
            )
        }
    }
}

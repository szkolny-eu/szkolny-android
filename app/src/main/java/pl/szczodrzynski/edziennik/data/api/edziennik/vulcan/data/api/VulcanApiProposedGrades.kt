package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.api

import com.google.gson.JsonArray
import pl.szczodrzynski.edziennik.HOUR
import pl.szczodrzynski.edziennik.asJsonObjectList
import pl.szczodrzynski.edziennik.data.api.VULCAN_API_ENDPOINT_GRADES_PROPOSITIONS
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_API_GRADES_SUMMARY
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanApi
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_SEMESTER1_FINAL
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_SEMESTER1_PROPOSED
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_SEMESTER2_FINAL
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_SEMESTER2_PROPOSED
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.getJsonArray
import pl.szczodrzynski.edziennik.getJsonObject
import pl.szczodrzynski.edziennik.utils.Utils

class VulcanApiProposedGrades(override val data: DataVulcan,
                              override val lastSync: Long?,
                              val onSuccess: (endpointId: Int) -> Unit
) : VulcanApi(data, lastSync) {
    companion object {
        const val TAG = "VulcanApiProposedGrades"
    }

    init { data.profile?.also { profile ->

        apiGet(TAG, VULCAN_API_ENDPOINT_GRADES_PROPOSITIONS, parameters = mapOf(
                "IdUczen" to data.studentId,
                "IdOkresKlasyfikacyjny" to data.studentSemesterId
        )) { json, _ ->
            val grades = json.getJsonObject("Data")

            grades.getJsonArray("OcenyPrzewidywane")?.let {
                processGradeList(it, isFinal = false)
            }

            grades.getJsonArray("OcenyKlasyfikacyjne")?.let {
                processGradeList(it, isFinal = true)
            }

            data.setSyncNext(ENDPOINT_VULCAN_API_GRADES_SUMMARY, 6*HOUR)
            onSuccess(ENDPOINT_VULCAN_API_GRADES_SUMMARY)
        }
    } ?: onSuccess(ENDPOINT_VULCAN_API_GRADES_SUMMARY) }

    private fun processGradeList(grades: JsonArray, isFinal: Boolean) {
        grades.asJsonObjectList()?.forEach { grade ->
            val name = grade.get("Wpis").asString
            val value = Utils.getGradeValue(name)
            val subjectId = grade.get("IdPrzedmiot").asLong

            val id = subjectId * -100 - data.studentSemesterNumber

            val color = Utils.getVulcanGradeColor(name)

            val gradeObject = Grade(
                    profileId = profileId,
                    id = id,
                    name = name,
                    type = if (data.studentSemesterNumber == 1) {
                        if (isFinal) TYPE_SEMESTER1_FINAL else TYPE_SEMESTER1_PROPOSED
                    } else {
                        if (isFinal) TYPE_SEMESTER2_FINAL else TYPE_SEMESTER2_PROPOSED
                    },
                    value = value,
                    weight = 0f,
                    color = color,
                    category = "",
                    description = null,
                    comment = null,
                    semester = data.studentSemesterNumber,
                    teacherId = -1,
                    subjectId = subjectId
            )

            data.gradeList.add(gradeObject)
            data.metadataList.add(Metadata(
                    profileId,
                    Metadata.TYPE_GRADE,
                    gradeObject.id,
                    data.profile?.empty ?: false,
                    data.profile?.empty ?: false
            ))
        }
    }
}

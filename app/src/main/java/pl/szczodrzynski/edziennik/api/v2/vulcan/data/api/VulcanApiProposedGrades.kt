package pl.szczodrzynski.edziennik.api.v2.vulcan.data.api

import com.google.gson.JsonArray
import pl.szczodrzynski.edziennik.HOUR
import pl.szczodrzynski.edziennik.api.v2.VULCAN_API_ENDPOINT_GRADES_PROPOSITIONS
import pl.szczodrzynski.edziennik.api.v2.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.api.v2.vulcan.ENDPOINT_VULCAN_API_GRADES_SUMMARY
import pl.szczodrzynski.edziennik.api.v2.vulcan.data.VulcanApi
import pl.szczodrzynski.edziennik.asJsonObjectList
import pl.szczodrzynski.edziennik.data.db.modules.grades.Grade
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.getJsonArray
import pl.szczodrzynski.edziennik.getJsonObject
import pl.szczodrzynski.edziennik.utils.Utils

class VulcanApiProposedGrades(override val data: DataVulcan, val onSuccess: () -> Unit) : VulcanApi(data) {
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
            onSuccess()
        }
    } ?: onSuccess()}

    private fun processGradeList(grades: JsonArray, isFinal: Boolean) {
        grades.asJsonObjectList()?.forEach { grade ->
            val name = grade.get("Wpis").asString
            val value = Utils.getGradeValue(name)
            val subjectId = grade.get("IdPrzedmiot").asLong

            val id = subjectId * -100 - data.studentSemesterNumber

            val color = Utils.getVulcanGradeColor(name)

            val gradeObject = Grade(
                    profileId,
                    id,
                    "",
                    color,
                    "",
                    name,
                    value,
                    0f,
                    data.studentSemesterNumber,
                    -1,
                    subjectId
            )
            if (data.studentSemesterNumber == 1) {
                gradeObject.type = if (isFinal) Grade.TYPE_SEMESTER1_FINAL else Grade.TYPE_SEMESTER1_PROPOSED
            } else {
                gradeObject.type = if (isFinal) Grade.TYPE_SEMESTER2_FINAL else Grade.TYPE_SEMESTER2_PROPOSED
            }
            data.gradeList.add(gradeObject)
            data.metadataList.add(Metadata(
                    profileId,
                    Metadata.TYPE_GRADE,
                    gradeObject.id,
                    data.profile?.empty ?: false,
                    data.profile?.empty ?: false,
                    System.currentTimeMillis()
            ))
        }
    }
}

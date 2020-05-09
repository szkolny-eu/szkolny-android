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
import pl.szczodrzynski.edziennik.data.db.entity.Profile
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

        val semesterId = data.studentSemesterId
        val semesterNumber = data.studentSemesterNumber
        if (semesterNumber == 2 && lastSync ?: 0 < profile.dateSemester1Start.inMillis) {
            getProposedGrades(profile, semesterId - 1, semesterNumber - 1) {
                getProposedGrades(profile, semesterId, semesterNumber) {
                    finish()
                }
            }
        }
        else {
            getProposedGrades(profile, semesterId, semesterNumber) {
                finish()
            }
        }

    } ?: onSuccess(ENDPOINT_VULCAN_API_GRADES_SUMMARY) }

    private fun finish() {
        data.setSyncNext(ENDPOINT_VULCAN_API_GRADES_SUMMARY, 6*HOUR)
        onSuccess(ENDPOINT_VULCAN_API_GRADES_SUMMARY)
    }

    private fun getProposedGrades(profile: Profile, semesterId: Int, semesterNumber: Int, onSuccess: () -> Unit) {
        apiGet(TAG, VULCAN_API_ENDPOINT_GRADES_PROPOSITIONS, parameters = mapOf(
                "IdUczen" to data.studentId,
                "IdOkresKlasyfikacyjny" to semesterId
        )) { json, _ ->
            val grades = json.getJsonObject("Data")

            grades.getJsonArray("OcenyPrzewidywane")?.let {
                processGradeList(it, semesterNumber, isFinal = false)
            }

            grades.getJsonArray("OcenyKlasyfikacyjne")?.let {
                processGradeList(it, semesterNumber, isFinal = true)
            }

            onSuccess()
        }
    }

    private fun processGradeList(grades: JsonArray, semesterNumber: Int, isFinal: Boolean) {
        grades.asJsonObjectList().forEach { grade ->
            val name = grade.get("Wpis").asString
            val value = Utils.getGradeValue(name)
            val subjectId = grade.get("IdPrzedmiot").asLong

            val id = subjectId * -100 - semesterNumber

            val color = Utils.getVulcanGradeColor(name)

            val gradeObject = Grade(
                    profileId = profileId,
                    id = id,
                    name = name,
                    type = if (semesterNumber == 1) {
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
                    semester = semesterNumber,
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

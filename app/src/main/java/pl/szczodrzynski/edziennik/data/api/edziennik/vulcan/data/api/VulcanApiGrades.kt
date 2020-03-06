/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-19
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.VULCAN_API_ENDPOINT_GRADES
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.DataVulcan
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.ENDPOINT_VULCAN_API_GRADES
import pl.szczodrzynski.edziennik.data.api.edziennik.vulcan.data.VulcanApi
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_NORMAL
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import java.text.DecimalFormat
import kotlin.math.roundToInt

class VulcanApiGrades(override val data: DataVulcan,
                      override val lastSync: Long?,
                      val onSuccess: (endpointId: Int) -> Unit
) : VulcanApi(data, lastSync) {
    companion object {
        const val TAG = "VulcanApiGrades"
    }

    init { data.profile?.also { profile ->

        apiGet(TAG, VULCAN_API_ENDPOINT_GRADES, parameters = mapOf(
                "IdUczen" to data.studentId,
                "IdOkresKlasyfikacyjny" to data.studentSemesterId
        )) { json, _ ->
            val grades = json.getJsonArray("Data")

            grades?.forEach { gradeEl ->
                val grade = gradeEl.asJsonObject

                val id = grade.getLong("Id") ?: return@forEach
                val categoryId = grade.getLong("IdKategoria") ?: -1
                val category = data.gradeCategories.singleOrNull{ it.categoryId == categoryId }?.text
                        ?: ""
                val teacherId = grade.getLong("IdPracownikD") ?: -1
                val subjectId = grade.getLong("IdPrzedmiot") ?: -1
                val description = grade.getString("Opis")
                val comment = grade.getString("Komentarz")
                var value = grade.getFloat("Wartosc")
                var weight = grade.getFloat("WagaOceny") ?: 0.0f
                val modificatorValue = grade.getFloat("WagaModyfikatora")
                val numerator = grade.getFloat("Licznik")
                val denominator = grade.getFloat("Mianownik")
                val addedDate = (grade.getLong("DataModyfikacji") ?: return@forEach) * 1000

                var finalDescription = ""

                var name = when (numerator != null && denominator != null) {
                    true -> {
                        value = numerator / denominator
                        finalDescription += DecimalFormat("#.##").format(numerator) +
                                "/" + DecimalFormat("#.##").format(denominator)
                        weight = 0.0f
                        (value * 100).roundToInt().toString() + "%"
                    }
                    else -> {
                        if (value != null) modificatorValue?.also { value += it }
                        else weight = 0.0f

                        grade.getString("Wpis") ?: ""
                    }
                }

                comment?.also {
                    if (name == "") name = it
                    else finalDescription = (if (finalDescription == "") "" else " ") + it
                }

                description?.also {
                    finalDescription = (if (finalDescription == "") "" else " - ") + it
                }

                val color = when (name) {
                    "1-", "1", "1+" -> 0xffd65757
                    "2-", "2", "2+" -> 0xff9071b3
                    "3-", "3", "3+" -> 0xffd2ab24
                    "4-", "4", "4+" -> 0xff50b6d6
                    "5-", "5", "5+" -> 0xff2cbd92
                    "6-", "6", "6+" -> 0xff91b43c
                    else -> 0xff3D5F9C
                }.toInt()

                val gradeObject = Grade(
                        profileId = profileId,
                        id = id,
                        name = name,
                        type = TYPE_NORMAL,
                        value = value ?: 0.0f,
                        weight = weight,
                        color = color,
                        category = category,
                        description = finalDescription,
                        comment = null,
                        semester = data.studentSemesterNumber,
                        teacherId = teacherId,
                        subjectId = subjectId
                )

                data.gradeList.add(gradeObject)
                data.metadataList.add(Metadata(
                        profileId,
                        Metadata.TYPE_GRADE,
                        id,
                        profile.empty,
                        profile.empty,
                        addedDate

                ))
            }

            data.toRemove.add(DataRemoveModel.Grades.semesterWithType(data.studentSemesterNumber, Grade.TYPE_NORMAL))
            data.setSyncNext(ENDPOINT_VULCAN_API_GRADES, SYNC_ALWAYS)
            onSuccess(ENDPOINT_VULCAN_API_GRADES)
        }
    } ?: onSuccess(ENDPOINT_VULCAN_API_GRADES) }
}

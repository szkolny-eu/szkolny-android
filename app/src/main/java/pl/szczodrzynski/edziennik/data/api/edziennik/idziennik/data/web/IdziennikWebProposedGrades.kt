/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-28.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.web

import pl.szczodrzynski.edziennik.asJsonObjectList
import pl.szczodrzynski.edziennik.data.api.ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA
import pl.szczodrzynski.edziennik.data.api.IDZIENNIK_WEB_MISSING_GRADES
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.ENDPOINT_IDZIENNIK_WEB_PROPOSED_GRADES
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.IdziennikWeb
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Grade
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_DESCRIPTIVE
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_SEMESTER1_PROPOSED
import pl.szczodrzynski.edziennik.data.db.entity.Grade.Companion.TYPE_YEAR_PROPOSED
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.getJsonArray
import pl.szczodrzynski.edziennik.getJsonObject
import pl.szczodrzynski.edziennik.getString

class IdziennikWebProposedGrades(override val data: DataIdziennik,
                                 override val lastSync: Long?,
                                 val onSuccess: (endpointId: Int) -> Unit
) : IdziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "IdziennikWebProposedGrades"
    }

    init { data.profile?.also { profile ->
        webApiGet(TAG, IDZIENNIK_WEB_MISSING_GRADES, mapOf(
                "idPozDziennika" to data.registerId
        )) { result ->
            val json = result.getJsonObject("d") ?: run {
                data.error(ApiError(TAG, ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA)
                        .withApiResponse(result))
                return@webApiGet
            }
            val manager = data.app.gradesManager

            json.getJsonArray("Przedmioty")?.asJsonObjectList()?.forEach { subject ->
                val subjectName = subject.getString("Przedmiot") ?: return@forEach
                val subjectObject = data.getSubject(subjectName, null, subjectName)

                val semester1Proposed = subject.getString("OcenaSem1") ?: ""
                val semester1Value = manager.getGradeValue(semester1Proposed)
                val semester1Id = subjectObject.id * (-100) - 1
                val semester1Type =
                        if (semester1Value == 0f) TYPE_DESCRIPTIVE
                        else TYPE_SEMESTER1_PROPOSED
                val semester1Name = when {
                    semester1Value == 0f -> " "
                    semester1Value % 1.0f == 0f -> semester1Value.toInt().toString()
                    else -> semester1Value.toString()
                }
                val semester1Color =
                        if (semester1Value == 0f) 0xff536dfe.toInt()
                        else -1

                val semester2Proposed = subject.getString("OcenaSem2") ?: ""
                val semester2Value = manager.getGradeValue(semester2Proposed)
                val semester2Id = subjectObject.id * (-100) - 2
                val semester2Type =
                        if (semester2Value == 0f) TYPE_DESCRIPTIVE
                        else TYPE_YEAR_PROPOSED
                val semester2Name = when {
                    semester2Value == 0f -> " "
                    semester2Value % 1.0f == 0f -> semester2Value.toInt().toString()
                    else -> semester2Value.toString()
                }
                val semester2Color =
                        if (semester2Value == 0f) 0xffff4081.toInt()
                        else -1

                if (semester1Proposed != "") {
                    val gradeObject = Grade(
                            profileId = profileId,
                            id = semester1Id,
                            name = semester1Name,
                            type = semester1Type,
                            value = semester1Value,
                            weight = 0f,
                            color = semester1Color,
                            category = if (semester1Value == 0f) "Ocena opisowa semestralna" else null,
                            description = if (semester1Value == 0f) semester1Proposed else null,
                            comment = null,
                            semester = 1,
                            teacherId = -1,
                            subjectId = subjectObject.id
                    )

                    val addedDate = if (data.profile.empty)
                        data.profile.dateSemester1Start.inMillis
                    else
                        System.currentTimeMillis()

                    data.gradeList.add(gradeObject)
                    data.metadataList.add(Metadata(
                            profileId,
                            Metadata.TYPE_GRADE,
                            gradeObject.id,
                            profile.empty,
                            profile.empty,
                            addedDate
                    ))
                }

                if (semester2Proposed != "") {
                    val gradeObject = Grade(
                            profileId = profileId,
                            id = semester2Id,
                            name = semester2Name,
                            type = semester2Type,
                            value = semester2Value,
                            weight = 0f,
                            color = semester2Color,
                            category = if (semester2Value == 0f) "Ocena opisowa końcoworoczna" else null,
                            description = if (semester2Value == 0f) semester2Proposed else null,
                            comment = null,
                            semester = 2,
                            teacherId = -1,
                            subjectId = subjectObject.id
                    )

                    val addedDate = if (data.profile.empty)
                        data.profile.dateSemester2Start.inMillis
                    else
                        System.currentTimeMillis()

                    data.gradeList.add(gradeObject)
                    data.metadataList.add(Metadata(
                            profileId,
                            Metadata.TYPE_GRADE,
                            gradeObject.id,
                            profile.empty,
                            profile.empty,
                            addedDate
                    ))
                }
            }

            data.toRemove.addAll(listOf(TYPE_SEMESTER1_PROPOSED, TYPE_YEAR_PROPOSED).map {
                DataRemoveModel.Grades.semesterWithType(profile.currentSemester, it)
            })
            data.setSyncNext(ENDPOINT_IDZIENNIK_WEB_PROPOSED_GRADES, SYNC_ALWAYS)
            onSuccess(ENDPOINT_IDZIENNIK_WEB_PROPOSED_GRADES)
        }
    } ?: onSuccess(ENDPOINT_IDZIENNIK_WEB_PROPOSED_GRADES) }
}

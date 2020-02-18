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
import pl.szczodrzynski.edziennik.data.db.entity.Grade.TYPE_SEMESTER1_PROPOSED
import pl.szczodrzynski.edziennik.data.db.entity.Grade.TYPE_YEAR_PROPOSED
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.getJsonArray
import pl.szczodrzynski.edziennik.getJsonObject
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.utils.Utils.getWordGradeValue

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

            json.getJsonArray("Przedmioty")?.asJsonObjectList()?.forEach { subject ->
                val subjectName = subject.getString("Przedmiot") ?: return@forEach
                val subjectObject = data.getSubject(subjectName, null, subjectName)

                val semester1Proposed = subject.getString("OcenaSem1") ?: ""
                val semester1Value = getWordGradeValue(semester1Proposed)
                val semester1Id = subjectObject.id * (-100) - 1

                val semester2Proposed = subject.getString("OcenaSem2") ?: ""
                val semester2Value = getWordGradeValue(semester2Proposed)
                val semester2Id = subjectObject.id * (-100) - 2

                if (semester1Proposed != "") {
                    val gradeObject = Grade(
                            profileId,
                            semester1Id,
                            "",
                            -1,
                            "",
                            semester1Value.toString(),
                            semester1Value.toFloat(),
                            0f,
                            1,
                            -1,
                            subjectObject.id
                    ).apply {
                        type = TYPE_SEMESTER1_PROPOSED
                    }

                    data.gradeList.add(gradeObject)
                    data.metadataList.add(Metadata(
                            profileId,
                            Metadata.TYPE_GRADE,
                            gradeObject.id,
                            profile.empty,
                            profile.empty,
                            System.currentTimeMillis()
                    ))
                }

                if (semester2Proposed != "") {
                    val gradeObject = Grade(
                            profileId,
                            semester2Id,
                            "",
                            -1,
                            "",
                            semester2Value.toString(),
                            semester2Value.toFloat(),
                            0f,
                            2,
                            -1,
                            subjectObject.id
                    ).apply {
                        type = TYPE_YEAR_PROPOSED
                    }

                    data.gradeList.add(gradeObject)
                    data.metadataList.add(Metadata(
                            profileId,
                            Metadata.TYPE_GRADE,
                            gradeObject.id,
                            profile.empty,
                            profile.empty,
                            System.currentTimeMillis()
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

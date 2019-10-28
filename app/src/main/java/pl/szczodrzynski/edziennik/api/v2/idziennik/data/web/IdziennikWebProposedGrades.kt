/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-28.
 */

package pl.szczodrzynski.edziennik.api.v2.idziennik.data.web

import pl.szczodrzynski.edziennik.api.v2.ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA
import pl.szczodrzynski.edziennik.api.v2.IDZIENNIK_WEB_MISSING_GRADES
import pl.szczodrzynski.edziennik.api.v2.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.api.v2.idziennik.ENDPOINT_IDZIENNIK_WEB_PROPOSED_GRADES
import pl.szczodrzynski.edziennik.api.v2.idziennik.data.IdziennikWeb
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.grades.Grade
import pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_SEMESTER1_PROPOSED
import pl.szczodrzynski.edziennik.data.db.modules.grades.Grade.TYPE_YEAR_PROPOSED
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.getJsonObject
import pl.szczodrzynski.edziennik.utils.Utils.getWordGradeValue

class IdziennikWebProposedGrades(override val data: DataIdziennik,
                         val onSuccess: () -> Unit) : IdziennikWeb(data) {
    companion object {
        private const val TAG = "IdziennikWebProposedGrades"
    }

    init {
        webApiGet(TAG, IDZIENNIK_WEB_MISSING_GRADES, mapOf(
                "idPozDziennika" to data.registerId
        )) { result ->
            val json = result.getJsonObject("d") ?: run {
                data.error(ApiError(TAG, ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA)
                        .withApiResponse(result))
                return@webApiGet
            }

            val jSubjects = json.getAsJsonArray("Przedmioty")
            for (jSubjectEl in jSubjects) {
                val jSubject = jSubjectEl.getAsJsonObject()
                // jSubject
                val rSubject = data.getSubject(jSubject.get("Przedmiot").getAsString(), -1, jSubject.get("Przedmiot").getAsString())
                val semester1Proposed = jSubject.get("OcenaSem1").getAsString()
                val semester2Proposed = jSubject.get("OcenaSem2").getAsString()
                val semester1Value = getWordGradeValue(semester1Proposed)
                val semester2Value = getWordGradeValue(semester2Proposed)
                val semester1Id = rSubject.id * -100 - 1
                val semester2Id = rSubject.id * -100 - 2

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
                            rSubject.id)

                    gradeObject.type = TYPE_SEMESTER1_PROPOSED

                    data.gradeList.add(gradeObject)
                    data.metadataList.add(Metadata(
                            profileId,
                            Metadata.TYPE_GRADE,
                            gradeObject.id,
                            profile?.empty ?: false,
                            profile?.empty ?: false,
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
                            rSubject.id)

                    gradeObject.type = TYPE_YEAR_PROPOSED

                    data.gradeList.add(gradeObject)
                    data.metadataList.add(Metadata(
                            profileId,
                            Metadata.TYPE_GRADE,
                            gradeObject.id,
                            profile?.empty ?: false,
                            profile?.empty ?: false,
                            System.currentTimeMillis()
                    ))
                }
            }

            data.setSyncNext(ENDPOINT_IDZIENNIK_WEB_PROPOSED_GRADES, SYNC_ALWAYS)
            onSuccess()
        }
    }
}

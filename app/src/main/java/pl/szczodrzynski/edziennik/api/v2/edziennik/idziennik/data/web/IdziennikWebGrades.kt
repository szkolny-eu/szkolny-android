/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-28.
 */

package pl.szczodrzynski.edziennik.api.v2.edziennik.idziennik.data.web

import android.graphics.Color
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.v2.ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA
import pl.szczodrzynski.edziennik.api.v2.IDZIENNIK_WEB_GRADES
import pl.szczodrzynski.edziennik.api.v2.edziennik.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.api.v2.edziennik.idziennik.ENDPOINT_IDZIENNIK_WEB_GRADES
import pl.szczodrzynski.edziennik.api.v2.edziennik.idziennik.data.IdziennikWeb
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.grades.Grade
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.utils.models.Date

class IdziennikWebGrades(override val data: DataIdziennik,
                            val onSuccess: () -> Unit) : IdziennikWeb(data) {
    companion object {
        private const val TAG = "IdziennikWebGrades"
    }

    init {
        webApiGet(TAG, IDZIENNIK_WEB_GRADES, mapOf(
                "idPozDziennika" to data.registerId
        )) { result ->
            val json = result.getJsonObject("d") ?: run {
                data.error(ApiError(TAG, ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA)
                        .withApiResponse(result))
                return@webApiGet
            }

            json.getJsonArray("Przedmioty")?.asJsonObjectList()?.onEach { subjectJson ->
                val subject = data.getSubject(
                        subjectJson.getString("Przedmiot") ?: return@onEach,
                        subjectJson.getLong("IdPrzedmiotu") ?: return@onEach,
                        subjectJson.getString("Przedmiot") ?: return@onEach
                )
                subjectJson.getJsonArray("Oceny")?.asJsonObjectList()?.forEach { grade ->
                    val id = grade.getLong("idK") ?: return@forEach
                    val category = grade.getString("Kategoria") ?: ""
                    val name = grade.getString("Ocena") ?: "?"
                    val semester = grade.getInt("Semestr") ?: 1
                    val teacher = data.getTeacherByLastFirst(grade.getString("Wystawil") ?: return@forEach)

                    val countToAverage = grade.getBoolean("DoSredniej") ?: true
                    var value = grade.getFloat("WartoscDoSred") ?: 0.0f
                    val weight = if (countToAverage)
                        grade.getFloat("Waga") ?: 0.0f
                    else
                        0.0f

                    val gradeColor = grade.getString("Kolor") ?: ""
                    var colorInt = 0xff2196f3.toInt()
                    if (gradeColor.isNotEmpty()) {
                        colorInt = Color.parseColor("#$gradeColor")
                    }

                    val gradeObject = Grade(
                            profileId,
                            id,
                            category,
                            colorInt,
                            "",
                            name,
                            value,
                            weight,
                            semester,
                            teacher.id,
                            subject.id)

                    when (grade.getInt("Typ")) {
                        0 -> {
                            val history = grade.getJsonArray("Historia")?.asJsonObjectList()
                            if (history?.isNotEmpty() == true) {
                                var sum = gradeObject.value * gradeObject.weight
                                var count = gradeObject.weight
                                for (historyItem in history) {
                                    val countToTheAverage = historyItem.getBoolean("DoSredniej") ?: false
                                    value = historyItem.get("WartoscDoSred").asFloat
                                    val weight = historyItem.get("Waga").asFloat

                                    if (value > 0 && countToTheAverage) {
                                        sum += value * weight
                                        count += weight
                                    }

                                    val historyObject = Grade(
                                            profileId,
                                            gradeObject.id * -1,
                                            historyItem.get("Kategoria").asString,
                                            Color.parseColor("#" + historyItem.get("Kolor").asString),
                                            historyItem.get("Uzasadnienie").asString,
                                            historyItem.get("Ocena").asString,
                                            value,
                                            if (value > 0f && countToTheAverage) weight * -1f else 0f,
                                            historyItem.get("Semestr").asInt,
                                            teacher.id,
                                            subject.id)
                                    historyObject.parentId = gradeObject.id

                                    val addedDate = historyItem.getString("Data_wystaw")?.let { Date.fromY_m_d(it).inMillis } ?: System.currentTimeMillis()

                                    data.gradeList.add(historyObject)
                                    data.metadataList.add(Metadata(
                                            profileId,
                                            Metadata.TYPE_GRADE,
                                            historyObject.id,
                                            true,
                                            true,
                                            addedDate
                                    ))
                                }
                                // update the current grade's value with an average of all historical grades and itself
                                if (sum > 0 && count > 0) {
                                    gradeObject.value = sum / count
                                }
                                gradeObject.isImprovement = true // gradeObject is the improved grade. Originals are historyObjects
                            }
                        }
                        1 -> {
                            gradeObject.type = Grade.TYPE_SEMESTER1_FINAL
                            gradeObject.name = name
                            gradeObject.weight = 0f
                        }
                        2 -> {
                            gradeObject.type = Grade.TYPE_YEAR_FINAL
                            gradeObject.name = name
                            gradeObject.weight = 0f
                        }
                    }

                    val addedDate = grade.getString("Data_wystaw")?.let { Date.fromY_m_d(it).inMillis } ?: System.currentTimeMillis()

                    data.gradeList.add(gradeObject)
                    data.metadataList.add(
                            Metadata(
                                    profileId,
                                    Metadata.TYPE_GRADE,
                                    id,
                                    data.profile?.empty ?: false,
                                    data.profile?.empty ?: false,
                                    addedDate
                            ))
                }
            }

            data.setSyncNext(ENDPOINT_IDZIENNIK_WEB_GRADES, SYNC_ALWAYS)
            onSuccess()
        }
    }
}

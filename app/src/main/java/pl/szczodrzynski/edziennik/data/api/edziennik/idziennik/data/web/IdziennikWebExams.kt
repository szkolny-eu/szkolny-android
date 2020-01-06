/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-28.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.web

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA
import pl.szczodrzynski.edziennik.data.api.IDZIENNIK_WEB_EXAMS
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.ENDPOINT_IDZIENNIK_WEB_EXAMS
import pl.szczodrzynski.edziennik.data.api.edziennik.idziennik.data.IdziennikWeb
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.utils.models.Date

class IdziennikWebExams(override val data: DataIdziennik,
                        val onSuccess: () -> Unit) : IdziennikWeb(data) {
    companion object {
        private const val TAG = "IdziennikWebExams"
    }

    private var examsYear = Date.getToday().year
    private var examsMonth = Date.getToday().month
    private var examsMonthsChecked = 0
    private var examsNextMonthChecked = false // TO DO temporary // no more // idk

    init {
        getExams()
    }

    private fun getExams() {
        val param = JsonObject().apply {
            addProperty("strona", 1)
            addProperty("iloscNaStrone", "99")
            addProperty("iloscRekordow", -1)
            addProperty("kolumnaSort", "ss.Nazwa,sp.Data_sprawdzianu")
            addProperty("kierunekSort", 0)
            addProperty("maxIloscZaznaczonych", 0)
            addProperty("panelFiltrow", 0)
        }

        webApiGet(TAG, IDZIENNIK_WEB_EXAMS, mapOf(
                "idP" to data.registerId,
                "rok" to examsYear,
                "miesiac" to examsMonth,
                "param" to param
        )) { result ->
            val json = result.getJsonObject("d") ?: run {
                data.error(ApiError(TAG, ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA)
                        .withApiResponse(result))
                return@webApiGet
            }

            json.getJsonArray("ListK")?.asJsonObjectList()?.forEach { exam ->
                val id = exam.getLong("_recordId") ?: return@forEach
                val examDate = Date.fromY_m_d(exam.getString("data") ?: return@forEach)
                val subjectName = exam.getString("przedmiot") ?: return@forEach
                val subjectId = data.getSubject(subjectName, null, subjectName).id
                val teacherName = exam.getString("wpisal") ?: return@forEach
                val teacherId = data.getTeacherByLastFirst(teacherName).id
                val topic = exam.getString("zakres") ?: ""

                val lessonList = data.db.timetableDao().getForDateNow(profileId, examDate)
                val startTime = lessonList.firstOrNull { it.subjectId == subjectId }?.startTime

                val eventType = when (exam.getString("rodzaj")) {
                    "sprawdzian/praca klasowa" -> Event.TYPE_EXAM
                    else -> Event.TYPE_SHORT_QUIZ
                }

                val eventObject = Event(
                        profileId,
                        id,
                        examDate,
                        startTime,
                        topic,
                        -1,
                        eventType,
                        false,
                        teacherId,
                        subjectId,
                        data.teamClass?.id ?: -1
                )

                data.eventList.add(eventObject)
                data.metadataList.add(Metadata(
                        profileId,
                        Metadata.TYPE_EVENT,
                        eventObject.id,
                        profile?.empty ?: false,
                        profile?.empty ?: false,
                        System.currentTimeMillis()
                ))
            }

            if (profile?.empty == true && examsMonthsChecked < 3 /* how many months backwards to check? */) {
                examsMonthsChecked++
                examsMonth--
                if (examsMonth < 1) {
                    examsMonth = 12
                    examsYear--
                }
                getExams()
            } else if (!examsNextMonthChecked /* get also one month forward */) {
                val showDate = Date.getToday().stepForward(0, 1, 0)
                examsYear = showDate.year
                examsMonth = showDate.month
                examsNextMonthChecked = true
                getExams()
            } else {
                data.toRemove.add(DataRemoveModel.Events.futureExceptType(Event.TYPE_HOMEWORK))

                data.setSyncNext(ENDPOINT_IDZIENNIK_WEB_EXAMS, SYNC_ALWAYS)
                onSuccess()
            }
        }
    }
}

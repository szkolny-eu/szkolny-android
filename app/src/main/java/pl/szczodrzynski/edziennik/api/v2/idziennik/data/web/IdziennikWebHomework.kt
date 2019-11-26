/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-25
 */

package pl.szczodrzynski.edziennik.api.v2.idziennik.data.web

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.v2.ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA
import pl.szczodrzynski.edziennik.api.v2.IDZIENNIK_WEB_HOMEWORK
import pl.szczodrzynski.edziennik.api.v2.idziennik.DataIdziennik
import pl.szczodrzynski.edziennik.api.v2.idziennik.ENDPOINT_IDZIENNIK_WEB_HOMEWORK
import pl.szczodrzynski.edziennik.api.v2.idziennik.data.IdziennikWeb
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.api.v2.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.events.Event
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.utils.models.Date

class IdziennikWebHomework(override val data: DataIdziennik,
                           val onSuccess: () -> Unit) : IdziennikWeb(data) {
    companion object {
        private const val TAG = "IdziennikWebHomework"
    }

    init {
        val param = JsonObject().apply {
            addProperty("strona", 1)
            addProperty("iloscNaStrone", 997)
            addProperty("iloscRekordow", -1)
            addProperty("kolumnaSort", "DataZadania")
            addProperty("kierunekSort", 0)
            addProperty("maxIloscZaznaczonych", 0)
            addProperty("panelFiltrow", 0)
        }

        webApiGet(TAG, IDZIENNIK_WEB_HOMEWORK, mapOf(
                "idP" to data.registerId,
                "data" to Date.getToday().stringY_m_d,
                "wszystkie" to true,
                "param" to param
        )) { result ->
            val json = result.getJsonObject("d") ?: run {
                data.error(ApiError(TAG, ERROR_IDZIENNIK_WEB_REQUEST_NO_DATA)
                        .withApiResponse(result))
                return@webApiGet
            }

            json.getJsonArray("ListK")?.asJsonObjectList()?.forEach { homework ->
                val id = homework.getLong("_recordId") ?: return@forEach
                val eventDate = Date.fromY_m_d(homework.getString("dataO") ?: return@forEach)
                val subjectName = homework.getString("przed") ?: return@forEach
                val subjectId = data.getSubject(subjectName, null, subjectName).id
                val teacherName = homework.getString("usr") ?: return@forEach
                val teacherId = data.getTeacherByLastFirst(teacherName).id
                val lessonList = data.db.timetableDao().getForDateNow(profileId, eventDate)
                val startTime = lessonList.firstOrNull { it.subjectId == subjectId }?.displayStartTime
                val topic = homework.getString("tytul") ?: ""

                val seen = when (profile?.empty) {
                    true -> true
                    else -> eventDate < Date.getToday()
                }


                val eventObject = Event(
                        profileId,
                        id,
                        eventDate,
                        startTime,
                        topic,
                        -1,
                        Event.TYPE_HOMEWORK,
                        false,
                        teacherId,
                        subjectId,
                        data.teamClass?.id ?: -1
                )

                data.eventList.add(eventObject)
                data.metadataList.add(Metadata(
                        profileId,
                        Metadata.TYPE_HOMEWORK,
                        eventObject.id,
                        seen,
                        seen,
                        System.currentTimeMillis()
                ))
            }

            data.toRemove.add(DataRemoveModel.Events.futureWithType(Event.TYPE_HOMEWORK))

            data.setSyncNext(ENDPOINT_IDZIENNIK_WEB_HOMEWORK, SYNC_ALWAYS)
            onSuccess()
        }
    }
}

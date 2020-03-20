/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-22.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.synergia

import org.jsoup.Jsoup
import pl.szczodrzynski.edziennik.HOUR
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_HOMEWORK
import pl.szczodrzynski.edziennik.data.api.POST
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_SYNERGIA_HOMEWORK
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusSynergia
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.get
import pl.szczodrzynski.edziennik.singleOrNull
import pl.szczodrzynski.edziennik.utils.models.Date

class LibrusSynergiaHomework(override val data: DataLibrus,
                             override val lastSync: Long?,
                             val onSuccess: (endpointId: Int) -> Unit
) : LibrusSynergia(data, lastSync) {
    companion object {
        const val TAG = "LibrusSynergiaHomework"
    }

    init { data.profile?.also { profile ->
        synergiaGet(TAG, "moje_zadania", method = POST, parameters = mapOf(
                "dataOd" to
                        if (profile.empty)
                            profile.getSemesterStart(1).stringY_m_d
                        else
                            Date.getToday().stringY_m_d,
                "dataDo" to Date.getToday().stepForward(0, 0, 7).stringY_m_d,
                "przedmiot" to -1

        )) { text ->
            val doc = Jsoup.parse(text)

            doc.select("table.myHomeworkTable > tbody").firstOrNull()?.also { homeworkTable ->
                val homeworkElements = homeworkTable.children()

                val graphElements = doc.select("table[border].center td[align=left] tbody").first().children()

                homeworkElements.forEachIndexed { i, el ->
                    val elements = el.children()

                    val subjectName = elements[0].text().trim()
                    val subjectId = data.subjectList.singleOrNull { it.longName == subjectName }?.id
                            ?: -1
                    val teacherName = elements[1].text().trim()
                    val teacherId = data.teacherList.singleOrNull { teacherName == it.fullName }?.id
                            ?: -1
                    val topic = elements[2].text().trim()
                    val addedDate = Date.fromY_m_d(elements[4].text().trim())
                    val eventDate = Date.fromY_m_d(elements[6].text().trim())
                    val id = "/podglad/([0-9]+)'".toRegex().find(
                            elements[9].select("input").attr("onclick")
                    )?.get(1)?.toLong() ?: return@forEachIndexed

                    val lessons = data.db.timetableDao().getForDateNow(profileId, eventDate)
                    val startTime = lessons.firstOrNull { it.subjectId == subjectId }?.startTime

                    /*val moreInfo = graphElements[2 * i + 1].select("td[title]")
                            .attr("title").trim()*/

                    var description = ""

                    graphElements.forEach { graphEl ->
                        graphEl.select("td[title]")?.also {
                            val title = it.attr("title")
                            val r = "Temat: (.*?)<br.?/>Data udostępnienia: (.*?)<br.?/>Termin wykonania: (.*?)<br.?/>Treść: (.*)"
                                    .toRegex(RegexOption.DOT_MATCHES_ALL).find(title) ?: return@forEach
                            val gTopic = r[1].trim()
                            val gAddedDate = Date.fromY_m_d(r[2].trim())
                            val gEventDate = Date.fromY_m_d(r[3].trim())
                            if (gTopic == topic && gAddedDate == addedDate && gEventDate == eventDate) {
                                description = r[4].replace("<br.?/>".toRegex(), "\n").trim()
                                return@forEach
                            }
                        }
                    }

                    val seen = when (profile.empty) {
                        true -> true
                        else -> eventDate < Date.getToday()
                    }

                    val eventObject = Event(
                            profileId,
                            id,
                            eventDate,
                            startTime,
                            "$topic\n$description",
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
                            id,
                            seen,
                            seen,
                            addedDate.inMillis
                    ))
                }
            }

            data.toRemove.add(DataRemoveModel.Events.futureWithType(Event.TYPE_HOMEWORK))

            // because this requires a synergia login (2 more requests!!!) sync this every few hours or if explicit :D
            data.setSyncNext(ENDPOINT_LIBRUS_SYNERGIA_HOMEWORK, 5 * HOUR, DRAWER_ITEM_HOMEWORK)
            onSuccess(ENDPOINT_LIBRUS_SYNERGIA_HOMEWORK)
        }
    } ?: onSuccess(ENDPOINT_LIBRUS_SYNERGIA_HOMEWORK) }
}

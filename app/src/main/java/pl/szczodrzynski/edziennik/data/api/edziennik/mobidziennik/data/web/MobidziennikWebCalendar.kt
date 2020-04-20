/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-10.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.web

import com.google.gson.JsonParser
import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.ENDPOINT_MOBIDZIENNIK_WEB_CALENDAR
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.MobidziennikWeb
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.getString
import pl.szczodrzynski.edziennik.utils.Utils.crc16
import pl.szczodrzynski.edziennik.utils.models.Date
import java.util.*

class MobidziennikWebCalendar(override val data: DataMobidziennik,
                              override val lastSync: Long?,
                              val onSuccess: (endpointId: Int) -> Unit
) : MobidziennikWeb(data, lastSync) {
    companion object {
        private const val TAG = "MobidziennikWebCalendar"
    }

    init {
        webGet(TAG, "/dziennik/kalendarzklasowy") { text ->
            MobidziennikLuckyNumberExtractor(data, text)

            Regexes.MOBIDZIENNIK_CLASS_CALENDAR.find(text)?.let {
                val events = JsonParser().parse(it.groupValues[1]).asJsonArray
                for (eventEl in events) {
                    val event = eventEl.asJsonObject

                    val idStr = event.getString("id")
                    if (idStr?.startsWith("kalendarz;") != true) {
                        continue
                    }
                    val idParts = idStr.split(";")
                    if (idParts.size < 2) {
                        continue
                    }
                    var id = idParts[2].toLongOrNull() ?: -1
                    val studentId = idParts[1].toIntOrNull() ?: continue
                    if (studentId != data.studentId)
                        continue

                    val dateString = event.getString("start") ?: continue
                    val eventDate = Date.fromY_m_d(dateString)

                    val eventType = when (event.getString("color")?.toLowerCase(Locale.getDefault())) {
                        "#c54449" -> Event.TYPE_SHORT_QUIZ
                        "#ab0001" -> Event.TYPE_EXAM
                        "#008928" -> Event.TYPE_CLASS_EVENT
                        "#b66000" -> Event.TYPE_EXCURSION
                        else -> Event.TYPE_INFORMATION
                    }

                    val title = event.getString("title")
                    val comment = event.getString("comment")

                    var topic = title ?: ""
                    if (title != comment) {
                        topic += "\n" + comment
                    }

                    if (id == -1L) {
                        id = crc16(topic.toByteArray()).toLong()
                    }

                    val eventObject = Event(
                            profileId = profileId,
                            id = id,
                            date = eventDate, time = null,
                            topic = topic,
                            color = null,
                            type = eventType,
                            teacherId = -1,
                            subjectId = -1,
                            teamId = data.teamClass?.id ?: -1
                    )

                    data.eventList.add(eventObject)
                    data.metadataList.add(
                            Metadata(
                                    profileId,
                                    Metadata.TYPE_EVENT,
                                    eventObject.id,
                                    profile?.empty ?: false,
                                    profile?.empty ?: false,
                                    System.currentTimeMillis() /* no addedDate here though */
                            ))
                }
            }

            data.setSyncNext(ENDPOINT_MOBIDZIENNIK_WEB_CALENDAR, SYNC_ALWAYS)
            onSuccess(ENDPOINT_MOBIDZIENNIK_WEB_CALENDAR)
        }
    }
}

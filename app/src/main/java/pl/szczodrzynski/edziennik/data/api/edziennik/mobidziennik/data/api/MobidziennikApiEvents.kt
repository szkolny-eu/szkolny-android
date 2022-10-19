/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-11.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.api

import androidx.core.util.contains
import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.enums.MetadataType
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class MobidziennikApiEvents(val data: DataMobidziennik, rows: List<String>) {
    init {
        for (row in rows) {
            if (row.isEmpty())
                continue
            val cols = row.split("|")

            val teamId = cols[2].toLong()
            if (data.teamList.contains(teamId)) {

                val id = cols[0].toLong()
                val teacherId = cols[1].toLong()
                val subjectId = cols[3].toLong()
                var type = Event.TYPE_DEFAULT
                var topic = cols[5].trim()
                Regexes.MOBIDZIENNIK_EVENT_TYPE.find(topic)?.let {
                    val typeText = it.groupValues[1]
                    when (typeText) {
                        "sprawdzian" -> type = Event.TYPE_EXAM
                        "kartkówka" -> type = Event.TYPE_SHORT_QUIZ
                    }
                    topic = topic.replace("($typeText)", "").trim()
                }
                val eventDate = Date.fromYmd(cols[4])
                val startTime = Time.fromYmdHm(cols[6])
                val format = SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
                val addedDate = try {
                    format.parse(cols[7]).time
                } catch (e: ParseException) {
                    e.printStackTrace()
                    System.currentTimeMillis()
                }


                val eventObject = Event(
                        profileId = data.profileId,
                        id = id,
                        date = eventDate,
                        time = startTime,
                        topic = topic,
                        color = null,
                        type = type,
                        teacherId = teacherId,
                        subjectId = subjectId,
                        teamId = teamId,
                        addedDate = addedDate
                )

                data.eventList.add(eventObject)
                data.metadataList.add(
                        Metadata(
                                data.profileId,
                                MetadataType.EVENT,
                                id,
                                data.profile?.empty ?: false,
                                data.profile?.empty ?: false
                        ))
            }
        }

        data.toRemove.add(DataRemoveModel.Events.futureWithType(Event.TYPE_DEFAULT))
        data.toRemove.add(DataRemoveModel.Events.futureWithType(Event.TYPE_EXAM))
        data.toRemove.add(DataRemoveModel.Events.futureWithType(Event.TYPE_SHORT_QUIZ))
    }
}

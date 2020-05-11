/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-11.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.api

import android.text.Html
import androidx.core.util.contains
import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.api.models.DataRemoveModel
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class MobidziennikApiHomework(val data: DataMobidziennik, rows: List<String>) {
    init {
        for (row in rows) {
            if (row.isEmpty())
                continue
            val cols = row.split("|")

            val teamId = cols[5].toLong()
            if (data.teamList.contains(teamId)) {

                val id = cols[0].toLong()
                val teacherId = cols[7].toLong()
                val subjectId = cols[6].toLong()
                val topic = Html.fromHtml(cols[1])?.toString()?.trim() ?: ""
                val eventDate = Date.fromYmd(cols[2])
                val startTime = Time.fromYmdHm(cols[3])

                val eventObject = Event(
                        profileId = data.profileId,
                        id = id,
                        date = eventDate,
                        time = startTime,
                        topic = topic,
                        color = null,
                        type = Event.TYPE_HOMEWORK,
                        teacherId = teacherId,
                        subjectId = subjectId,
                        teamId = teamId
                )

                data.eventList.add(eventObject)
                data.metadataList.add(
                        Metadata(
                                data.profileId,
                                Metadata.TYPE_HOMEWORK,
                                id,
                                data.profile?.empty ?: false,
                                data.profile?.empty ?: false
                        ))
            }
        }

        data.toRemove.add(DataRemoveModel.Events.futureWithType(Event.TYPE_HOMEWORK))
    }
}

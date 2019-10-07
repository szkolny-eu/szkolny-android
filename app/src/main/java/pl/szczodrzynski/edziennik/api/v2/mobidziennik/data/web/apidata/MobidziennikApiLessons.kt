/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-7.
 */

package pl.szczodrzynski.edziennik.api.v2.mobidziennik.data.web.apidata

import pl.szczodrzynski.edziennik.api.v2.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class MobidziennikApiLessons(val data: DataMobidziennik, rows: List<String>) {
    init {
        data.mobiLessons.clear()
        for (row in rows) {
            if (row.isEmpty())
                continue
            val cols = row.split("|")

            val id = cols[0].toLong()
            val subjectId = cols[1].toLong()
            val teacherId = cols[2].toLong()
            val teamId = cols[3].toLong()
            val topic = cols[4]
            val date = Date.fromYmd(cols[5])
            val startTime = Time.fromYmdHm(cols[6])
            val endTime = Time.fromYmdHm(cols[7])
            val presentCount = cols[8].toInt()
            val absentCount = cols[9].toInt()
            val lessonNumber = cols[10].toInt()
            val signed = cols[11]

            val lesson = DataMobidziennik.MobiLesson(
                    id,
                    subjectId,
                    teacherId,
                    teamId,
                    topic,
                    date,
                    startTime,
                    endTime,
                    presentCount,
                    absentCount,
                    lessonNumber,
                    signed
            )
            data.mobiLessons.add(lesson)
        }
    }
}
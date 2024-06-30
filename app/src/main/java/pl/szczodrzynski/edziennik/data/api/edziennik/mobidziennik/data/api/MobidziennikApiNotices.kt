/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-11.
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.data.api

import pl.szczodrzynski.edziennik.data.api.edziennik.mobidziennik.DataMobidziennik
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.Notice
import pl.szczodrzynski.edziennik.data.enums.MetadataType
import pl.szczodrzynski.edziennik.utils.models.Date

class MobidziennikApiNotices(val data: DataMobidziennik, rows: List<String>) {
    init { run {
        for (row in rows) {
            if (row.isEmpty())
                continue
            val cols = row.split("|")

            val studentId = cols[2].toInt()
            if (studentId != data.studentId)
                return@run

            val id = cols[0].toLong()
            val text = cols[4]
            val semester = cols[6].toInt()
            val type = when (cols[3]) {
                "0" -> Notice.TYPE_NEGATIVE
                "1" -> Notice.TYPE_POSITIVE
                "3" -> Notice.TYPE_NEUTRAL
                else -> Notice.TYPE_NEUTRAL
            }
            val teacherId = cols[5].toLong()
            val addedDate = Date.fromYmd(cols[7]).inMillis

            val noticeObject = Notice(
                    profileId = data.profileId,
                    id = id,
                    type = type,
                    semester = semester,
                    text = text,
                    category = null,
                    points = null,
                    teacherId = teacherId,
                    addedDate = addedDate
            )

            data.noticeList.add(noticeObject)
            data.metadataList.add(
                    Metadata(
                            data.profileId,
                            MetadataType.NOTICE,
                            id,
                            data.profile?.empty ?: false,
                            data.profile?.empty ?: false
                    ))
        }
    }}
}

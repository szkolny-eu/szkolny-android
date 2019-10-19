/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-13
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.ENDPOINT_LIBRUS_API_ANNOUNCEMENTS
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.modules.announcements.Announcement
import pl.szczodrzynski.edziennik.data.db.modules.api.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.utils.Utils
import pl.szczodrzynski.edziennik.utils.models.Date

class LibrusApiAnnouncements(override val data: DataLibrus,
                             val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiAnnouncements"
    }

    init {
        apiGet(TAG, "SchoolNotices") { json ->
            val announcements = json.getJsonArray("SchoolNotices")

            announcements?.forEach { announcementEl ->
                val announcement = announcementEl.asJsonObject

                val id = Utils.crc16(announcement.getString("Id")?.toByteArray()
                        ?: return@forEach).toLong()
                val subject = announcement.getString("Subject") ?: ""
                val text = announcement.getString("Content") ?: ""
                val startDate = Date.fromY_m_d(announcement.getString("StartDate"))
                val endDate = Date.fromY_m_d(announcement.getString("EndDate"))
                val teacherId = announcement.getJsonObject("AddedBy")?.getLong("Id") ?: -1
                val addedDate = Date.fromIso(announcement.getString("CreationDate"))
                val read = announcement.getBoolean("WasRead") ?: false

                val announcementObject = Announcement(
                        profileId,
                        id,
                        subject,
                        text,
                        startDate,
                        endDate,
                        teacherId
                )

                data.announcementList.add(announcementObject)
                data.metadataList.add(Metadata(
                        profileId,
                        Metadata.TYPE_ANNOUNCEMENT,
                        id,
                        read,
                        read,
                        addedDate
                ))
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_ANNOUNCEMENTS, SYNC_ALWAYS)
            onSuccess()
        }
    }
}

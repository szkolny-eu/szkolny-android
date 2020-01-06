/*
 * Copyright (c) Kacper Ziubryniewicz 2019-10-13
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.ENDPOINT_LIBRUS_API_ANNOUNCEMENTS
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.entity.Announcement
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.utils.models.Date

class LibrusApiAnnouncements(override val data: DataLibrus,
                             val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiAnnouncements"
    }

    init { data.profile?.also { profile ->
        apiGet(TAG, "SchoolNotices") { json ->
            val announcements = json.getJsonArray("SchoolNotices")?.asJsonObjectList()

            announcements?.forEach { announcement ->
                val longId = announcement.getString("Id") ?: return@forEach
                val id = longId.crc32()
                val subject = announcement.getString("Subject") ?: ""
                val text = announcement.getString("Content") ?: ""
                val startDate = Date.fromY_m_d(announcement.getString("StartDate"))
                val endDate = Date.fromY_m_d(announcement.getString("EndDate"))
                val teacherId = announcement.getJsonObject("AddedBy")?.getLong("Id") ?: -1
                val addedDate = announcement.getString("CreationDate")?.let { Date.fromIso(it) }
                        ?: System.currentTimeMillis()
                val read = announcement.getBoolean("WasRead") ?: false

                val announcementObject = Announcement(
                        profileId,
                        id,
                        subject,
                        text,
                        startDate,
                        endDate,
                        teacherId,
                        longId
                )

                data.announcementList.add(announcementObject)
                data.setSeenMetadataList.add(Metadata(
                        profileId,
                        Metadata.TYPE_ANNOUNCEMENT,
                        id,
                        read,
                        profile.empty || read,
                        addedDate
                ))
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_ANNOUNCEMENTS, SYNC_ALWAYS)
            onSuccess()
        }
    }}
}

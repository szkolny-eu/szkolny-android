/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-27
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.api

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.DataLibrus
import pl.szczodrzynski.edziennik.data.api.edziennik.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.api.events.AnnouncementGetEvent
import pl.szczodrzynski.edziennik.data.db.modules.announcements.AnnouncementFull
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata

class LibrusApiAnnouncementMarkAsRead(
        override val data: DataLibrus,
        private val announcement: AnnouncementFull,
        val onSuccess: () -> Unit
) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiAnnouncementMarkAsRead"
    }

    init {
        apiGet(TAG, "SchoolNotices/MarkAsRead/${announcement.idString}") {
            announcement.seen = true

            EventBus.getDefault().postSticky(AnnouncementGetEvent(announcement))

            data.setSeenMetadataList.add(Metadata(
                    profileId,
                    Metadata.TYPE_ANNOUNCEMENT,
                    announcement.id,
                    announcement.seen,
                    announcement.notified,
                    announcement.addedDate
            ))
            onSuccess()
        }
    }
}

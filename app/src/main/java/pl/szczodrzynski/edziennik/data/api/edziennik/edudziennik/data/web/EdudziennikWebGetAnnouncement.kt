/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-26
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.web

import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.data.api.Regexes
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.DataEdudziennik
import pl.szczodrzynski.edziennik.data.api.edziennik.edudziennik.data.EdudziennikWeb
import pl.szczodrzynski.edziennik.data.api.events.AnnouncementGetEvent
import pl.szczodrzynski.edziennik.data.db.modules.announcements.AnnouncementFull
import pl.szczodrzynski.edziennik.get

class EdudziennikWebGetAnnouncement(
        override val data: DataEdudziennik,
        private val announcement: AnnouncementFull,
        val onSuccess: () -> Unit
) : EdudziennikWeb(data) {
    companion object {
        const val TAG = "EdudziennikWebGetAnnouncement"
    }

    init {
        webGet(TAG, "Announcement/${announcement.idString}") { text ->
            val description = Regexes.EDUDZIENNIK_ANNOUNCEMENT_DESCRIPTION.find(text)?.get(1)?.trim() ?: ""

            announcement.text = description

            EventBus.getDefault().postSticky(AnnouncementGetEvent(announcement))

            data.announcementList.add(announcement)
            onSuccess()
        }
    }
}

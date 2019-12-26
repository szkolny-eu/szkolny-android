/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-26
 */

package pl.szczodrzynski.edziennik.data.api.events

import pl.szczodrzynski.edziennik.data.db.modules.announcements.AnnouncementFull

data class AnnouncementGetEvent(val announcement: AnnouncementFull)

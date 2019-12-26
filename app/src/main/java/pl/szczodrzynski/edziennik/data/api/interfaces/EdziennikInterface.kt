/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-29.
 */

package pl.szczodrzynski.edziennik.data.api.interfaces

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.data.db.modules.announcements.AnnouncementFull
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageFull

interface EdziennikInterface {
    fun sync(featureIds: List<Int>, viewId: Int? = null, arguments: JsonObject? = null)
    fun getMessage(message: MessageFull)
    fun markAllAnnouncementsAsRead()
    fun getAnnouncement(announcement: AnnouncementFull)
    fun getAttachment(message: Message, attachmentId: Long, attachmentName: String)
    fun firstLogin()
    fun cancel()
}

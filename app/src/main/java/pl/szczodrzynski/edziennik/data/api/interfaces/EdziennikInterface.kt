/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-29.
 */

package pl.szczodrzynski.edziennik.data.api.interfaces

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.data.db.full.AnnouncementFull
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.data.db.entity.Teacher

interface EdziennikInterface {
    fun sync(featureIds: List<Int>, viewId: Int? = null, arguments: JsonObject? = null)
    fun getMessage(message: MessageFull)
    fun sendMessage(recipients: List<Teacher>, subject: String, text: String)
    fun markAllAnnouncementsAsRead()
    fun getAnnouncement(announcement: AnnouncementFull)
    fun getAttachment(message: Message, attachmentId: Long, attachmentName: String)
    fun getRecipientList()
    fun firstLogin()
    fun cancel()
}

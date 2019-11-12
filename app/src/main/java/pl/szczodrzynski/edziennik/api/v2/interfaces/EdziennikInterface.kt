/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-29.
 */

package pl.szczodrzynski.edziennik.api.v2.interfaces

import com.google.gson.JsonObject
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageFull

interface EdziennikInterface {
    fun sync(featureIds: List<Int>, viewId: Int? = null, arguments: JsonObject? = null)
    fun getMessage(message: MessageFull)
    fun markAllAnnouncementsAsRead()
    fun firstLogin()
    fun cancel()
}

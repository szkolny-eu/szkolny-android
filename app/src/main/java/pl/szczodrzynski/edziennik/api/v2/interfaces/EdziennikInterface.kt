/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-29.
 */

package pl.szczodrzynski.edziennik.api.v2.interfaces

import com.google.gson.JsonObject

interface EdziennikInterface {
    fun sync(featureIds: List<Int>, viewId: Int? = null, arguments: JsonObject? = null)
    fun getMessage(messageId: Long)
    fun markAllAnnouncementsAsRead()
    fun firstLogin()
    fun cancel()
}

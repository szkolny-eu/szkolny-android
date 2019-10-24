/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-24.
 */

package pl.szczodrzynski.edziennik.api.v2.librus.data.api

import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.v2.librus.DataLibrus
import pl.szczodrzynski.edziennik.api.v2.librus.ENDPOINT_LIBRUS_API_EVENT_TYPES
import pl.szczodrzynski.edziennik.api.v2.librus.data.LibrusApi
import pl.szczodrzynski.edziennik.data.db.modules.events.EventType

class LibrusApiEventTypes(override val data: DataLibrus,
                           val onSuccess: () -> Unit) : LibrusApi(data) {
    companion object {
        const val TAG = "LibrusApiEventTypes"
    }

    init {
        apiGet(TAG, "HomeWorks/Categories") { json ->
            val eventTypes = json.getJsonArray("Categories").asJsonObjectList()

            eventTypes?.forEach { eventType ->
                val id = eventType.getLong("Id") ?: return@forEach
                val name = eventType.getString("Name") ?: ""
                val color = data.getColor(eventType.getJsonObject("Color")?.getInt("Id"))

                data.eventTypes.put(id, EventType(profileId, id, name, color))
            }

            data.setSyncNext(ENDPOINT_LIBRUS_API_EVENT_TYPES, 4*DAY)
            onSuccess()
        }
    }
}

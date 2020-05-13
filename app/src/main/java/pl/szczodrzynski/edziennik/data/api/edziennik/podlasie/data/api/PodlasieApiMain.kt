/*
 * Copyright (c) Kacper Ziubryniewicz 2020-5-12
 */

package pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.data.api

import pl.szczodrzynski.edziennik.asJsonObjectList
import pl.szczodrzynski.edziennik.data.api.PODLASIE_API_USER_ENDPOINT
import pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.DataPodlasie
import pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.ENDPOINT_PODLASIE_API_MAIN
import pl.szczodrzynski.edziennik.data.api.edziennik.podlasie.data.PodlasieApi
import pl.szczodrzynski.edziennik.data.db.entity.SYNC_ALWAYS
import pl.szczodrzynski.edziennik.getJsonArray

class PodlasieApiMain(override val data: DataPodlasie,
                      override val lastSync: Long?,
                      val onSuccess: (endpointId: Int) -> Unit) : PodlasieApi(data, lastSync) {
    companion object {
        const val TAG = "PodlasieApiTimetable"
    }

    init {
        apiGet(TAG, PODLASIE_API_USER_ENDPOINT) { json ->
            data.getTeam() // Save the class team when it doesn't exist.

            json.getJsonArray("Timetable")?.asJsonObjectList()?.let { PodlasieApiTimetable(data, it) }

            data.setSyncNext(ENDPOINT_PODLASIE_API_MAIN, SYNC_ALWAYS)
            onSuccess(ENDPOINT_PODLASIE_API_MAIN)
        }
    }
}

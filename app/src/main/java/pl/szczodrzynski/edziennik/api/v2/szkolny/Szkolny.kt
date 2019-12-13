/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-13
 */

package pl.szczodrzynski.edziennik.api.v2.szkolny

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.api.v2.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.data.db.modules.events.EventFull
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.data.db.modules.profiles.ProfileFull

class Szkolny(val app: App, val callback: EdziennikCallback) {

    private val api = SzkolnyApi(app)

    fun sync(profiles: List<ProfileFull>) {
        if (profiles.isNotEmpty()) {
            val events = api.getEvents(profiles)

            if (events.isNotEmpty()) {
                app.db.eventDao().addAll(events)
                app.db.metadataDao().addAllIgnore(events.map { event ->
                    Metadata(
                            event.profileId,
                            Metadata.TYPE_EVENT,
                            event.id,
                            event.seen,
                            event.notified,
                            event.addedDate
                    )
                })
            }
        }

        completed()
    }

    fun shareEvent(event: EventFull) {
        api.shareEvent(event)
        completed()
    }

    fun unshareEvent(event: EventFull) {
        api.unshareEvent(event)
        completed()
    }

    private fun completed() {
        callback.onCompleted()
    }
}

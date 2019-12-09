/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-7.
 */

package pl.szczodrzynski.edziennik.api.v2.events.task

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.api.v2.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.api.v2.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.data.db.modules.metadata.Metadata
import pl.szczodrzynski.edziennik.data.db.modules.profiles.ProfileFull

class ServerSyncTask : IApiTask(-1) {
    override fun prepare(app: App) {
        taskName = app.getString(R.string.edziennik_notification_api_notify_title) // TODO text
    }

    override fun cancel() {

    }

    fun run(app: App, profiles: List<ProfileFull>, taskCallback: EdziennikCallback) {
        val api = SzkolnyApi(app)

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

        taskCallback.onCompleted()
    }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-17.
 */

package pl.szczodrzynski.edziennik.data.api.task

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.data.db.entity.Metadata
import pl.szczodrzynski.edziennik.data.db.entity.Notification
import pl.szczodrzynski.edziennik.data.db.entity.Profile

class AppSync(val app: App, val notifications: MutableList<Notification>, val profiles: List<Profile>, val api: SzkolnyApi) {
    companion object {
        private const val TAG = "AppSync"
    }

    /**
     * Run the app sync, sending all pending notifications
     * and retrieving a list of shared events.
     *
     * Events are automatically saved to app database,
     * along with corresponding metadata objects.
     *
     * @return a number of events inserted to DB, possibly needing a notification
     */
    fun run(lastSyncTime: Long, markAsSeen: Boolean = false): Int {
        val blacklistedIds = app.db.eventDao().blacklistedIds
        val events = api.getEvents(profiles, notifications, blacklistedIds, lastSyncTime)

        app.config.sync.lastAppSync = System.currentTimeMillis()

        if (events.isNotEmpty()) {
            app.db.metadataDao().addAllIgnore(events.map { event ->
                Metadata(
                        event.profileId,
                        Metadata.TYPE_EVENT,
                        event.id,
                        markAsSeen || event.seen,
                        markAsSeen || event.notified,
                        event.addedDate
                )
            })
            return app.db.eventDao().addAll(events).size
        }
        return 0;
    }
}

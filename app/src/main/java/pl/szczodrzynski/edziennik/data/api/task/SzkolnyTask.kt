/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-7.
 */

package pl.szczodrzynski.edziennik.data.api.task

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.data.api.szkolny.Szkolny
import pl.szczodrzynski.edziennik.data.db.modules.events.EventFull
import pl.szczodrzynski.edziennik.data.db.modules.profiles.ProfileFull

class SzkolnyTask(val request: Any) : IApiTask(-1) {
    companion object {
        private const val TAG = "SzkolnyTask"

        fun sync(profiles: List<ProfileFull>) = SzkolnyTask(SyncRequest(profiles))
        fun shareEvent(event: EventFull) = SzkolnyTask(ShareEventRequest(event))
        fun unshareEvent(event: EventFull) = SzkolnyTask(UnshareEventRequest(event))
    }

    private lateinit var szkolny: Szkolny

    override fun prepare(app: App) {
        taskName = app.getString(R.string.edziennik_szkolny_api_sync_title)
    }

    override fun cancel() {
        // TODO
    }

    internal fun run(app: App, taskCallback: EdziennikCallback) {
        szkolny = Szkolny(app, taskCallback)

        when (request) {
            is SyncRequest -> szkolny.sync(request.profiles)
            is ShareEventRequest -> szkolny.shareEvent(request.event)
            is UnshareEventRequest -> szkolny.unshareEvent(request.event)
        }
    }

    data class SyncRequest(val profiles: List<ProfileFull>)
    data class ShareEventRequest(val event: EventFull)
    data class UnshareEventRequest(val event: EventFull)
}

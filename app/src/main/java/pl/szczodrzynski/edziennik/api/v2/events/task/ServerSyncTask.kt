/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-7.
 */

package pl.szczodrzynski.edziennik.api.v2.events.task

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.api.v2.interfaces.EdziennikCallback

class ServerSyncTask : IApiTask(-1) {
    override fun prepare(app: App) {
        taskName = app.getString(R.string.edziennik_notification_api_notify_title) // TODO text
    }

    override fun cancel() {

    }

    fun run(app: App, taskCallback: EdziennikCallback) {


        taskCallback.onCompleted()
    }
}
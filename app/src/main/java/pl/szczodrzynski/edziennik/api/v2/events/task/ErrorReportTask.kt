/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-1.
 */

package pl.szczodrzynski.edziennik.api.v2.events.task

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.api.v2.ApiService
import pl.szczodrzynski.edziennik.api.v2.EdziennikNotification
import pl.szczodrzynski.edziennik.api.v2.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.utils.Utils

class ErrorReportTask : IApiTask(-1) {
    override fun prepare(app: App) {
        taskName = app.getString(R.string.edziennik_notification_api_error_report_title)
    }

    override fun cancel() {

    }

    fun run(app: App, taskCallback: EdziennikCallback, notification: EdziennikNotification, errorList: MutableList<ApiError>) {
        errorList.forEach { error ->
            Utils.d(ApiService.TAG, "Error ${error.tag} profile ${error.profileId}: code ${error.errorCode}")
        }
        errorList.clear()

        taskCallback.onCompleted()
    }


}
/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-1.
 */

package pl.szczodrzynski.edziennik.data.api.task

import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.ApiService
import pl.szczodrzynski.edziennik.data.api.EdziennikNotification
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.utils.Utils
import timber.log.Timber

class ErrorReportTask : IApiTask(-1) {
    override fun prepare(app: App) {
        taskName = app.getString(R.string.edziennik_notification_api_error_report_title)
    }

    override fun cancel() {

    }

    fun run(app: App, taskCallback: EdziennikCallback, notification: EdziennikNotification, errorList: MutableList<ApiError>) {
        errorList.forEach { error ->
            Timber.d("Error ${error.tag} profile ${error.profileId}: code ${error.errorCode}")
        }
        errorList.clear()

        taskCallback.onCompleted()
    }


}

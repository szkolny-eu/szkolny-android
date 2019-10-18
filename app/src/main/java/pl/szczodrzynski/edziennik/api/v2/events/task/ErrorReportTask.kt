/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-1.
 */

package pl.szczodrzynski.edziennik.api.v2.events.task

import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.api.v2.ApiService
import pl.szczodrzynski.edziennik.api.v2.EdziennikNotification
import pl.szczodrzynski.edziennik.api.v2.models.ApiError
import pl.szczodrzynski.edziennik.api.v2.models.ApiTask
import pl.szczodrzynski.edziennik.utils.Utils

class ErrorReportTask : ApiTask(-1) {
    fun run(notification: EdziennikNotification, errorList: MutableList<ApiError>) {
        notification
                .setCurrentTask(taskId, null)
                .setProgressRes(R.string.edziennik_notification_api_error_report_title)
                .post()
        errorList.forEach { error ->
            Utils.d(ApiService.TAG, "Error ${error.tag} profile ${error.profileId}: code ${error.errorCode}")
        }
        errorList.clear()
        notification.setIdle().post()
    }
}
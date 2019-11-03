/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-1.
 */

package pl.szczodrzynski.edziennik.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import pl.szczodrzynski.edziennik.api.v2.ApiService
import pl.szczodrzynski.edziennik.api.v2.events.requests.ServiceCloseRequest
import pl.szczodrzynski.edziennik.api.v2.events.requests.SyncProfileRequest
import pl.szczodrzynski.edziennik.api.v2.events.requests.SyncRequest
import pl.szczodrzynski.edziennik.api.v2.events.requests.TaskCancelRequest

class SzkolnyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        when (intent?.extras?.getString("task", null)) {
            "ServiceCloseRequest" -> ApiService.startAndRequest(context, ServiceCloseRequest())
            "TaskCancelRequest" -> ApiService.startAndRequest(context, TaskCancelRequest(intent.extras?.getInt("taskId", -1) ?: return))
            "SyncRequest" -> ApiService.startAndRequest(context, SyncRequest())
            "SyncProfileRequest" -> ApiService.startAndRequest(context, SyncProfileRequest(intent.extras?.getInt("profileId", -1) ?: return))
        }
    }
}

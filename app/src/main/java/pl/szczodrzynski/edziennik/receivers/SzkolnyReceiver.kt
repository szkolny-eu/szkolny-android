/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-1.
 */

package pl.szczodrzynski.edziennik.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.api.v2.events.requests.ServiceCloseRequest
import pl.szczodrzynski.edziennik.api.v2.events.requests.SyncRequest
import pl.szczodrzynski.edziennik.api.v2.events.requests.TaskCancelRequest

class SzkolnyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.extras?.getString("task", null)) {
            "ServiceCloseRequest" -> EventBus.getDefault().post(ServiceCloseRequest())
            "TaskCancelRequest" -> EventBus.getDefault().post(TaskCancelRequest(intent.extras?.getInt("taskId", -1) ?: return))
            "SyncRequest" -> EventBus.getDefault().post(SyncRequest())
        }
    }
}
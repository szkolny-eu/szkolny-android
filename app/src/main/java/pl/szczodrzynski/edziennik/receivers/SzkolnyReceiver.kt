/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-1.
 */

package pl.szczodrzynski.edziennik.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.api.v2.ApiService
import pl.szczodrzynski.edziennik.api.v2.events.requests.*

class SzkolnyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        context?.startService(Intent(context, ApiService::class.java))
        when (intent?.extras?.getString("task", null)) {
            "ServiceCloseRequest" -> EventBus.getDefault().post(ServiceCloseRequest())
            "TaskCancelRequest" -> EventBus.getDefault().post(TaskCancelRequest(intent.extras?.getInt("taskId", -1) ?: return))
            "SyncRequest" -> EventBus.getDefault().post(SyncRequest())
            "SyncProfileRequest" -> EventBus.getDefault().post(SyncProfileRequest(intent.extras?.getInt("profileId", -1) ?: return))
            "AnnouncementsReadRequest" -> EventBus.getDefault().post(AnnouncementsReadRequest(intent.extras?.getInt("profileId", -1) ?: return))
        }
    }
}

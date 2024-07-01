/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-1.
 */

package pl.szczodrzynski.edziennik.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import pl.szczodrzynski.edziennik.data.api.ApiService
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.events.requests.ServiceCloseRequest
import pl.szczodrzynski.edziennik.data.api.events.requests.TaskCancelRequest

class SzkolnyReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION = "pl.szczodrzynski.edziennik.SZKOLNY_MAIN"
        fun getIntent(context: Context, extras: Bundle): Intent {
            val intent = Intent(context, SzkolnyReceiver::class.java)
            intent.putExtras(extras)
            return intent
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        when (intent?.extras?.getString("task", null)) {
            "ServiceCloseRequest" -> ApiService.startAndRequest(context, ServiceCloseRequest())
            "TaskCancelRequest" -> ApiService.startAndRequest(context, TaskCancelRequest(intent.extras?.getInt("taskId", -1) ?: return))
            "SyncRequest" -> EdziennikTask.sync().enqueue(context)
            "SyncProfileRequest" -> EdziennikTask.syncProfile(intent.extras?.getInt("profileId", -1) ?: return).enqueue(context)
        }
    }
}

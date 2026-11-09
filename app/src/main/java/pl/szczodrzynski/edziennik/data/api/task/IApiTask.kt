/*
 * Copyright (c) Kuba Szczodrzyński 2019-9-28.
 */

package pl.szczodrzynski.edziennik.data.api.task

import android.content.Context
import android.content.Intent
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.sync.SyncNowWorker
import pl.szczodrzynski.edziennik.data.api.ApiService
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import android.util.Log
import pl.szczodrzynski.edziennik.utils.Utils.d

abstract class IApiTask(open val profileId: Int) {
    var taskId: Int = 0
    var profile: Profile? = null
    var taskName: String? = null

    /**
     * A method called before running the task.
     * It is synchronous and its main task is
     * to prepare the correct task name.
     */
    abstract fun prepare(app: App)
    abstract fun cancel()

    fun enqueue(context: Context) {
        enqueueAll(context, listOf(this))
    }

    override fun toString(): String {
        return "IApiTask(profileId=$profileId, taskId=$taskId, profile=$profile, taskName=$taskName)"
    }

    companion object {
        fun enqueueAll(context: Context, tasks: List<IApiTask>) {
            if (tasks.isEmpty())
                return
            if (!startApiService(context)) {
                enqueueInBackground(context, tasks)
                return
            }
            tasks.forEach {
                EventBus.getDefault().postSticky(it)
            }
        }

        /**
         * Try to start the [ApiService].
         *
         * Since Android 12 a foreground service can't be started
         * while the app is in background - it throws
         * ForegroundServiceStartNotAllowedException (an [IllegalStateException]).
         * Older versions throw [IllegalStateException] as well, when
         * starting a background service.
         *
         * @return false if the service could not be started
         */
        private fun startApiService(context: Context): Boolean {
            return try {
                Intent(context, ApiService::class.java).let {
                    if (SDK_INT >= O)
                        context.startForegroundService(it)
                    else
                        context.startService(it)
                }
                true
            } catch (e: IllegalStateException) {
                Log.w("IApiTask", "Can't start ApiService from background", e)
                false
            }
        }

        /**
         * Run the sync tasks using WorkManager, which is allowed
         * in background. Other tasks (e.g. sending messages) are
         * only started from the UI, so they can be skipped.
         */
        private fun enqueueInBackground(context: Context, tasks: List<IApiTask>) {
            var syncAll = false
            val profileIds = mutableSetOf<Int>()
            tasks.forEach { task ->
                if (task !is EdziennikTask) {
                    d("IApiTask", "Skipping $task - can't run it in background")
                    return@forEach
                }
                when (val request = task.request) {
                    is EdziennikTask.SyncRequest -> syncAll = true
                    is EdziennikTask.SyncProfileRequest -> profileIds += task.profileId
                    is EdziennikTask.SyncProfileListRequest -> profileIds += request.profileList
                    else -> d("IApiTask", "Skipping $task - can't run it in background")
                }
            }
            when {
                syncAll -> SyncNowWorker.enqueue(context, profileIds = null)
                profileIds.isNotEmpty() -> SyncNowWorker.enqueue(context, profileIds)
            }
        }
    }
}

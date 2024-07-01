/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-18.
 */

package pl.szczodrzynski.edziennik.core.work

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.*
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.szkolny.response.Update
import pl.szczodrzynski.edziennik.ext.DAY
import pl.szczodrzynski.edziennik.ext.formatDate
import pl.szczodrzynski.edziennik.utils.Utils
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class UpdateWorker(val context: Context, val params: WorkerParameters) : Worker(context, params), CoroutineScope {
    companion object {
        const val TAG = "UpdateWorker"

        /**
         * Schedule the sync job only if it's not already scheduled.
         */
        @SuppressLint("RestrictedApi")
        fun scheduleNext(app: App, rescheduleIfFailedFound: Boolean = true) {
            WorkerUtils.scheduleNext(app, rescheduleIfFailedFound) {
                rescheduleNext(app)
            }
        }

        /**
         * Cancel any existing sync jobs and schedule a new one.
         *
         * If [ConfigSync.enabled] is not true, just cancel every job.
         */
        fun rescheduleNext(app: App) {
            cancelNext(app)
            if (!app.config.sync.notifyAboutUpdates) {
                return
            }
            val syncInterval = 4 * DAY;

            val syncAt = System.currentTimeMillis() + syncInterval*1000
            Timber.d("Scheduling work at ${syncAt.formatDate()}")

            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val syncWorkRequest = OneTimeWorkRequestBuilder<UpdateWorker>()
                    .setInitialDelay(syncInterval, TimeUnit.SECONDS)
                    .setConstraints(constraints)
                    .addTag(TAG)
                    .build()

            WorkManager.getInstance(app).enqueue(syncWorkRequest)
        }

        /**
         * Cancel any scheduled sync job.
         */
        fun cancelNext(app: App) {
            Timber.d("Cancelling work by tag $TAG")
            WorkManager.getInstance(app).cancelAllWorkByTag(TAG)
        }
    }

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun doWork(): Result {
        Timber.d("Running worker ID ${params.id}")
        val app = context as App
        if (!app.config.sync.notifyAboutUpdates) {
            return Result.success()
        }

        val channel = if (App.devMode)
            Update.Type.BETA
        else
            Update.Type.RELEASE
        app.updateManager.checkNowSync(channel, notify = true)

        rescheduleNext(this.context)
        return Result.success()
    }
}

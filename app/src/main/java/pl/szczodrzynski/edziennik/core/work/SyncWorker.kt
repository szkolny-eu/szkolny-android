package pl.szczodrzynski.edziennik.core.work

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.*
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.ext.formatDate
import timber.log.Timber
import java.util.concurrent.TimeUnit

class SyncWorker(val context: Context, val params: WorkerParameters) : Worker(context, params) {
    companion object {
        const val TAG = "SyncWorker"

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
            val enableSync = app.config.sync.enabled
            if (!enableSync) {
                return
            }
            val onlyWifi = app.config.sync.onlyWifi
            val syncInterval = app.config.sync.interval.toLong()

            val syncAt = System.currentTimeMillis() + syncInterval*1000
            Timber.d("Scheduling work at ${syncAt.formatDate()}")

            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(
                            if (onlyWifi)
                                NetworkType.UNMETERED
                            else
                                NetworkType.CONNECTED)
                    .build()

            val syncWorkRequest = OneTimeWorkRequestBuilder<SyncWorker>()
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
            //WorkManager.getInstance(app).pruneWork() // do not prune the work in order to look for failed tasks
        }
    }

    override fun doWork(): Result {
        Timber.d("Running worker ID ${params.id}")
        EdziennikTask.sync().enqueue(context)
        rescheduleNext(context as App)
        return Result.success()
    }
}

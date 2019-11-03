package pl.szczodrzynski.edziennik.sync

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import androidx.work.*
import androidx.work.impl.WorkManagerImpl
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MINUTE
import pl.szczodrzynski.edziennik.api.v2.ApiService
import pl.szczodrzynski.edziennik.api.v2.events.requests.SyncRequest
import pl.szczodrzynski.edziennik.formatDate
import pl.szczodrzynski.edziennik.utils.Utils.d
import java.util.concurrent.TimeUnit

class SyncWorker(val context: Context, val params: WorkerParameters) : Worker(context, params) {
    companion object {
        const val TAG = "SyncWorker"

        /**
         * Schedule the sync job only if it's not already scheduled.
         */
        @SuppressLint("RestrictedApi")
        fun scheduleNext(app: App, rescheduleIfFailedFound: Boolean = true) {
            AsyncTask.execute {
                val workManager = WorkManager.getInstance(app) as WorkManagerImpl
                val scheduledWork = workManager.workDatabase.workSpecDao().scheduledWork
                scheduledWork.forEach {
                    d(TAG, "Work: ${it.id} at ${(it.periodStartTime+it.initialDelay).formatDate()}. State = ${it.state} (finished = ${it.state.isFinished})")
                }
                // remove finished work and other than SyncWorker
                scheduledWork.removeAll { it.workerClassName != SyncWorker::class.java.canonicalName || it.isPeriodic || it.state.isFinished }
                d(TAG, "Found ${scheduledWork.size} unfinished work")
                // remove all enqueued work that had to (but didn't) run at some point in the past (at least 1min ago)
                val failedWork = scheduledWork.filter { it.state == WorkInfo.State.ENQUEUED && it.periodStartTime+it.initialDelay < System.currentTimeMillis() - 1*MINUTE*1000 }
                d(TAG, "${failedWork.size} work requests failed to start (out of ${scheduledWork.size} requests)")
                if (rescheduleIfFailedFound) {
                    if (failedWork.isNotEmpty()) {
                        d(TAG, "App Manager detected!")
                        EventBus.getDefault().postSticky(AppManagerDetectedEvent(failedWork.map { it.periodStartTime + it.initialDelay }))
                    }
                    if (scheduledWork.size - failedWork.size < 1) {
                        d(TAG, "No pending work found, scheduling next:")
                        rescheduleNext(app)
                    }
                }
                else {
                    d(TAG, "NOT rescheduling: waiting to open the activity")
                    if (scheduledWork.size < 1) {
                        d(TAG, "No work found *at all*, scheduling next:")
                        rescheduleNext(app)
                    }
                }
            }
        }

        /**
         * Cancel any existing sync jobs and schedule a new one.
         *
         * If [registerSyncEnabled] is not true, just cancel every job.
         */
        fun rescheduleNext(app: App) {
            cancelNext(app)
            val enableSync = app.appConfig.registerSyncEnabled
            if (!enableSync) {
                return
            }
            val onlyWifi = app.appConfig.registerSyncOnlyWifi
            val syncInterval = app.appConfig.registerSyncInterval.toLong()

            val syncAt = System.currentTimeMillis() + syncInterval*1000
            d(TAG, "Scheduling work at ${syncAt.formatDate()}")

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
            d(TAG, "Cancelling work by tag $TAG")
            WorkManager.getInstance(app).cancelAllWorkByTag(TAG)
            //WorkManager.getInstance(app).pruneWork() // do not prune the work in order to look for failed tasks
        }
    }

    override fun doWork(): Result {
        d(TAG, "Running worker ID ${params.id}")
        ApiService.startAndRequest(context, SyncRequest())
        rescheduleNext(context as App)
        return Result.success()
    }
}
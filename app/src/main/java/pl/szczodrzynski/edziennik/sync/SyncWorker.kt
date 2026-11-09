package pl.szczodrzynski.edziennik.sync

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.*
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.api.task.BackgroundSync
import pl.szczodrzynski.edziennik.ext.formatDate
import android.util.Log
import pl.szczodrzynski.edziennik.utils.Utils.d
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/**
 * Periodic background sync.
 *
 * The sync is executed directly inside the worker (see [BackgroundSync]),
 * instead of starting the foreground [pl.szczodrzynski.edziennik.data.api.ApiService].
 * Starting a foreground service from the background is not allowed
 * since Android 12 - it threw an exception, which also prevented
 * the next sync from being scheduled.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
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

    override suspend fun doWork(): Result {
        val app = applicationContext as App
        d(TAG, "Running worker ID $id")
        try {
            BackgroundSync(app).run(profileIds = null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Background sync failed", e)
        } finally {
            // always schedule the next sync, even if this one failed
            rescheduleNext(app)
        }
        return Result.success()
    }
}

/**
 * One-time sync, executed as soon as there is network access.
 *
 * Used when the [pl.szczodrzynski.edziennik.data.api.ApiService]
 * can't be started because the app is in background
 * (e.g. a sync requested by a push notification).
 */
class SyncNowWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    companion object {
        const val TAG = "SyncNowWorker"
        private const val KEY_PROFILE_IDS = "profileIds"

        /**
         * @param profileIds profiles to sync, or null to sync
         * every profile with sync enabled
         */
        fun enqueue(context: Context, profileIds: Collection<Int>?) {
            d(TAG, "Enqueueing one-time background sync of ${profileIds ?: "all profiles"}")
            val data = Data.Builder()
            if (profileIds != null)
                data.putIntArray(KEY_PROFILE_IDS, profileIds.toIntArray())

            val request = OneTimeWorkRequestBuilder<SyncNowWorker>()
                    .setConstraints(Constraints.Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build())
                    .setInputData(data.build())
                    .addTag(TAG)
                    .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }

    override suspend fun doWork(): Result {
        val app = applicationContext as App
        val profileIds = inputData.getIntArray(KEY_PROFILE_IDS)?.toList()
        d(TAG, "Running one-time worker ID $id for ${profileIds ?: "all profiles"}")
        try {
            BackgroundSync(app).run(profileIds)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "One-time background sync failed", e)
        }
        return Result.success()
    }
}

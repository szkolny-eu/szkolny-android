/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-18.
 */

package pl.szczodrzynski.edziennik.core.work

import android.annotation.SuppressLint
import android.os.AsyncTask
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.impl.WorkManagerImpl
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.ext.MINUTE
import pl.szczodrzynski.edziennik.ext.formatDate
import pl.szczodrzynski.edziennik.utils.Utils
import timber.log.Timber

object WorkerUtils {
    /**
     * Schedule the sync job only if it's not already scheduled.
     */
    @SuppressLint("RestrictedApi")
    inline fun scheduleNext(app: App, rescheduleIfFailedFound: Boolean = true, crossinline onReschedule: () -> Unit) {
        AsyncTask.execute {
            // TODO fix and refactor this class
            val workManager = WorkManager.getInstance(app) as WorkManagerImpl
            val scheduledWork = workManager.workDatabase.workSpecDao().getScheduledWork().toMutableList()
            scheduledWork.forEach {
                Timber.d("Work: ${it.id} at ${it.calculateNextRunTime().formatDate()}. State = ${it.state} (finished = ${it.state.isFinished})")
            }
            // remove finished work and other than SyncWorker
            scheduledWork.removeAll { it.workerClassName != SyncWorker::class.java.canonicalName || it.isPeriodic || it.state.isFinished }
            Timber.d("Found ${scheduledWork.size} unfinished work")
            // remove all enqueued work that had to (but didn't) run at some point in the past (at least 1min ago)
            val failedWork = scheduledWork.filter { it.state == WorkInfo.State.ENQUEUED && it.calculateNextRunTime() < System.currentTimeMillis() - 1 * MINUTE * 1000 }
            Timber.d("${failedWork.size} work requests failed to start (out of ${scheduledWork.size} requests)")
            if (rescheduleIfFailedFound) {
                if (failedWork.isNotEmpty()) {
                    Timber.d("App Manager detected!")
                    EventBus.getDefault().postSticky(AppManagerDetectedEvent(failedWork.map { it.calculateNextRunTime() }))
                }
                if (scheduledWork.size - failedWork.size < 1) {
                    Timber.d("No pending work found, scheduling next:")
                    onReschedule()
                }
            } else {
                Timber.d("NOT rescheduling: waiting to open the activity")
                if (scheduledWork.size < 1) {
                    Timber.d("No work found *at all*, scheduling next:")
                    onReschedule()
                }
            }
        }
    }
}

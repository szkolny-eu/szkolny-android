/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-18.
 */

package pl.szczodrzynski.edziennik.sync

import android.annotation.SuppressLint
import android.os.AsyncTask
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.impl.WorkManagerImpl
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MINUTE
import pl.szczodrzynski.edziennik.formatDate
import pl.szczodrzynski.edziennik.utils.Utils

object WorkerUtils {
    /**
     * Schedule the sync job only if it's not already scheduled.
     */
    @SuppressLint("RestrictedApi")
    inline fun scheduleNext(app: App, rescheduleIfFailedFound: Boolean = true, crossinline onReschedule: () -> Unit) {
        AsyncTask.execute {
            val workManager = WorkManager.getInstance(app) as WorkManagerImpl
            val scheduledWork = workManager.workDatabase.workSpecDao().scheduledWork
            scheduledWork.forEach {
                Utils.d("WorkerUtils", "Work: ${it.id} at ${(it.periodStartTime + it.initialDelay).formatDate()}. State = ${it.state} (finished = ${it.state.isFinished})")
            }
            // remove finished work and other than SyncWorker
            scheduledWork.removeAll { it.workerClassName != SyncWorker::class.java.canonicalName || it.isPeriodic || it.state.isFinished }
            Utils.d("WorkerUtils", "Found ${scheduledWork.size} unfinished work")
            // remove all enqueued work that had to (but didn't) run at some point in the past (at least 1min ago)
            val failedWork = scheduledWork.filter { it.state == WorkInfo.State.ENQUEUED && it.periodStartTime + it.initialDelay < System.currentTimeMillis() - 1 * MINUTE * 1000 }
            Utils.d("WorkerUtils", "${failedWork.size} work requests failed to start (out of ${scheduledWork.size} requests)")
            if (rescheduleIfFailedFound) {
                if (failedWork.isNotEmpty()) {
                    Utils.d("WorkerUtils", "App Manager detected!")
                    EventBus.getDefault().postSticky(AppManagerDetectedEvent(failedWork.map { it.periodStartTime + it.initialDelay }))
                }
                if (scheduledWork.size - failedWork.size < 1) {
                    Utils.d("WorkerUtils", "No pending work found, scheduling next:")
                    onReschedule()
                }
            } else {
                Utils.d("WorkerUtils", "NOT rescheduling: waiting to open the activity")
                if (scheduledWork.size < 1) {
                    Utils.d("WorkerUtils", "No work found *at all*, scheduling next:")
                    onReschedule()
                }
            }
        }
    }
}

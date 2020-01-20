/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-18.
 */

package pl.szczodrzynski.edziennik.sync

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.Html
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.work.*
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.data.api.szkolny.response.Update
import pl.szczodrzynski.edziennik.utils.Utils
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
            Utils.d(TAG, "Scheduling work at ${syncAt.formatDate()}")

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
            Utils.d(TAG, "Cancelling work by tag $TAG")
            WorkManager.getInstance(app).cancelAllWorkByTag(TAG)
        }

        suspend fun runNow(app: App, overrideUpdate: Update? = null) {
            try {
                val update = overrideUpdate
                        ?: run {
                            val response = withContext(Dispatchers.Default) { SzkolnyApi(app).getUpdate("beta") }
                            if (response?.success != true) {
                                Toast.makeText(app, app.getString(R.string.notification_cant_check_update), Toast.LENGTH_SHORT).show()
                                return@run null
                            }
                            val updates = response.data
                            if (updates?.isNotEmpty() != true) {
                                app.config.update = null
                                Toast.makeText(app, app.getString(R.string.notification_no_update), Toast.LENGTH_SHORT).show()
                                return@run null
                            }
                            updates[0]
                        } ?: return

                app.config.update = update

                val notificationIntent = Intent(app, UpdateDownloaderService::class.java)
                val pendingIntent = PendingIntent.getService(app, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
                val notification = NotificationCompat.Builder(app, app.notifications.updatesKey)
                        .setContentTitle(app.getString(R.string.notification_updates_title))
                        .setContentText(app.getString(R.string.notification_updates_text, update.versionName))
                        .setTicker(app.getString(R.string.notification_updates_summary))
                        .setSmallIcon(R.drawable.ic_notification)
                        .setStyle(NotificationCompat.BigTextStyle()
                                .bigText(listOf(
                                        app.getString(R.string.notification_updates_text, update.versionName),
                                        update.releaseNotes?.let { Html.fromHtml(it) }
                                ).concat("\n")))
                        .setColor(0xff2196f3.toInt())
                        .setLights(0xFF00FFFF.toInt(), 2000, 2000)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setGroup(app.notifications.updatesKey)
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(false)
                        .build()
                (app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(app.notifications.updatesId, notification)

            } catch (ignore: Exception) { }
        }
    }

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun doWork(): Result {
        Utils.d(TAG, "Running worker ID ${params.id}")
        val app = context as App
        if (!app.config.sync.notifyAboutUpdates) {
            return Result.success()
        }

        launch {
            runNow(app)
        }

        rescheduleNext(this.context)
        return Result.success()
    }

    class JavaWrapper(app: App) : CoroutineScope {
        private val job = Job()
        override val coroutineContext: CoroutineContext
            get() = job + Dispatchers.Main
        init {
            launch {
                runNow(app)
            }
        }
    }
}

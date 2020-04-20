/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-1.
 */

package pl.szczodrzynski.edziennik.data.api

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.Bundle
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.receivers.SzkolnyReceiver
import kotlin.math.roundToInt


class EdziennikNotification(val app: App) {
    private val notificationManager by lazy { app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private val notificationBuilder: NotificationCompat.Builder by lazy {
        NotificationCompat.Builder(app, ApiService.NOTIFICATION_API_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(PRIORITY_MIN)
                .setOngoing(true)
                .setLocalOnly(true)
    }

    val notification: Notification
        get() = notificationBuilder.build()

    private var errorCount = 0
    private var criticalErrorCount = 0
    var serviceClosed = false

    private fun cancelPendingIntent(taskId: Int): PendingIntent {
        val intent = SzkolnyReceiver.getIntent(app, Bundle(
                "task" to "TaskCancelRequest",
                "taskId" to taskId
        ))
        return PendingIntent.getBroadcast(app, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT) as PendingIntent
    }
    private val closePendingIntent: PendingIntent
        get() {
            val intent = SzkolnyReceiver.getIntent(app, Bundle(
                    "task" to "ServiceCloseRequest"
            ))
            return PendingIntent.getBroadcast(app, 0, intent, 0) as PendingIntent
        }

    private fun errorCountText(): String? {
        var result = ""
        if (criticalErrorCount > 0) {
            result += app.resources.getQuantityString(R.plurals.critical_errors_format, criticalErrorCount, criticalErrorCount)
        }
        if (criticalErrorCount > 0 && errorCount > 0) {
            result += ", "
        }
        if (errorCount > 0) {
            result += app.resources.getQuantityString(R.plurals.normal_errors_format, errorCount, errorCount)
        }
        return if (result.isEmpty()) null else result
    }

    fun setIdle(): EdziennikNotification {
        notificationBuilder.setContentTitle(app.getString(R.string.edziennik_notification_api_title))
        notificationBuilder.setProgress(0, 0, false)
        notificationBuilder.apply {
            val str = app.getString(R.string.edziennik_notification_api_text)
            setStyle(NotificationCompat.BigTextStyle().bigText(str))
            setContentText(str)
        }
        setCloseAction()
        return this
    }

    fun addError(): EdziennikNotification {
        errorCount++
        return this
    }
    fun setCriticalError(): EdziennikNotification {
        criticalErrorCount++
        notificationBuilder.setContentTitle(app.getString(R.string.edziennik_notification_api_error_title))
        notificationBuilder.setProgress(0, 0, false)
        notificationBuilder.apply {
            val str = errorCountText()
            setStyle(NotificationCompat.BigTextStyle().bigText(str))
            setContentText(str)
        }
        setCloseAction()
        return this
    }

    fun setProgress(progress: Float): EdziennikNotification {
        notificationBuilder.setProgress(100, progress.roundToInt(), progress < 0f)
        return this
    }
    fun setProgressText(progressText: String?): EdziennikNotification {
        notificationBuilder.setContentTitle(progressText)
        return this
    }

    fun setCurrentTask(taskId: Int, progressText: String?): EdziennikNotification {
        notificationBuilder.setProgress(100, 0, true)
        notificationBuilder.setContentTitle(progressText)
        notificationBuilder.apply {
            val str = errorCountText()
            setStyle(NotificationCompat.BigTextStyle().bigText(str))
            setContentText(str)
        }
        setCancelAction(taskId)
        return this
    }

    fun setCloseAction(): EdziennikNotification {
        notificationBuilder.mActions.clear()
        notificationBuilder.addAction(
                NotificationCompat.Action(
                        R.drawable.ic_notification,
                        app.getString(R.string.edziennik_notification_api_close),
                        closePendingIntent
                ))
        return this
    }
    private fun setCancelAction(taskId: Int) {
        notificationBuilder.mActions.clear()
        notificationBuilder.addAction(
                NotificationCompat.Action(
                        R.drawable.ic_notification,
                        app.getString(R.string.edziennik_notification_api_cancel),
                        cancelPendingIntent(taskId)
                ))
    }

    fun post() {
        if (serviceClosed)
            return
        notificationManager.notify(app.notificationChannelsManager.sync.id, notification)
    }

}

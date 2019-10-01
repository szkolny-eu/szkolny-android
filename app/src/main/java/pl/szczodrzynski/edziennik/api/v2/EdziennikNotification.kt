/*
 * Copyright (c) Kuba Szczodrzyński 2019-10-1.
 */

package pl.szczodrzynski.edziennik.api.v2

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.app.NotificationCompat.PRIORITY_MIN
import pl.szczodrzynski.edziennik.R


class EdziennikNotification(val context: Context) {
    companion object {
        const val NOTIFICATION_ID = 20191001
    }

    private val notificationManager by lazy { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    private val notificationBuilder: NotificationCompat.Builder by lazy {
        NotificationCompat.Builder(context, ApiService.NOTIFICATION_API_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(PRIORITY_MIN)
                .setOngoing(true)
                .setLocalOnly(true)
    }

    val notification: Notification
        get() = notificationBuilder.build()

    private var errorCount = 0
    private var criticalErrorCount = 0

    private fun cancelPendingIntent(taskId: Int): PendingIntent {
        val intent = Intent("pl.szczodrzynski.edziennik.SZKOLNY_MAIN")
        intent.putExtra("task", "TaskCancelRequest")
        intent.putExtra("taskId", taskId)
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT) as PendingIntent
    }
    private val closePendingIntent: PendingIntent
        get() {
            val intent = Intent("pl.szczodrzynski.edziennik.SZKOLNY_MAIN")
            intent.putExtra("task", "ServiceCloseRequest")
            return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT) as PendingIntent
        }

    private fun errorCountText(): String? {
        var result = ""
        if (criticalErrorCount > 0) {
            result += context.resources.getQuantityString(R.plurals.critical_errors_format, criticalErrorCount, criticalErrorCount)
        }
        if (criticalErrorCount > 0 && errorCount > 0) {
            result += ", "
        }
        if (errorCount > 0) {
            result += context.resources.getQuantityString(R.plurals.normal_errors_format, errorCount, errorCount)
        }
        return if (result.isEmpty()) null else result
    }

    fun setIdle(): EdziennikNotification {
        notificationBuilder.setContentTitle(context.getString(R.string.edziennik_notification_api_title))
        notificationBuilder.setProgress(0, 0, false)
        notificationBuilder.apply {
            val str = context.getString(R.string.edziennik_notification_api_text)
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
        notificationBuilder.setContentTitle(context.getString(R.string.edziennik_notification_api_error_title))
        notificationBuilder.setProgress(0, 0, false)
        notificationBuilder.apply {
            val str = errorCountText()
            setStyle(NotificationCompat.BigTextStyle().bigText(str))
            setContentText(str)
        }
        setCloseAction()
        return this
    }

    fun setProgress(progress: Int): EdziennikNotification {
        notificationBuilder.setProgress(100, progress, false)
        return this
    }
    fun setProgressRes(progressRes: Int): EdziennikNotification {
        notificationBuilder.setContentTitle(context.getString(progressRes))
        return this
    }

    fun setCurrentTask(taskId: Int, profileName: String?): EdziennikNotification {
        notificationBuilder.setProgress(100, 0, false)
        notificationBuilder.setContentTitle(context.getString(R.string.edziennik_notification_api_sync_title_format, profileName))
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
                        context.getString(R.string.edziennik_notification_api_close),
                        closePendingIntent
                ))
        return this
    }
    private fun setCancelAction(taskId: Int) {
        notificationBuilder.mActions.clear()
        notificationBuilder.addAction(
                NotificationCompat.Action(
                        R.drawable.ic_notification,
                        context.getString(R.string.edziennik_notification_api_cancel),
                        cancelPendingIntent(taskId)
                ))
    }

    fun post() {
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

}
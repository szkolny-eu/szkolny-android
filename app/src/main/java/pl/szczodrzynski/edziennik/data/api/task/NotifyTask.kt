package pl.szczodrzynski.edziennik.data.api.task

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.Notifier.ID_NOTIFICATIONS
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.interfaces.EdziennikCallback
import pl.szczodrzynski.edziennik.data.db.modules.notification.getNotificationTitle
import pl.szczodrzynski.edziennik.utils.models.Notification
import kotlin.math.min

class NotifyTask : IApiTask(-1) {
    override fun prepare(app: App) {
        taskName = app.getString(R.string.edziennik_notification_api_notify_title)
    }

    override fun cancel() {

    }

    fun run(app: App, taskCallback: EdziennikCallback) {
        val list = app.db.notificationDao().getNotPostedNow()
        val notificationList = list.subList(0, min(15, list.size))

        var unreadCount = list.size

        for (notification in notificationList) {
            val intent = Intent(app, MainActivity::class.java)
            notification.fillIntent(intent)
            val pendingIntent = PendingIntent.getActivity(app, notification.id, intent, 0)
            val notificationBuilder = NotificationCompat.Builder(app, app.notifier.notificationGroup)
                    // title, text, type, date
                    .setContentTitle(notification.profileName)
                    .setContentText(notification.text)
                    .setSubText(app.getNotificationTitle(notification.type))
                    .setWhen(notification.addedDate)
                    .setTicker(app.getString(R.string.notification_ticker_format, Notification.stringType(app, notification.type)))
                    // icon, color, lights, priority
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(app.notifier.notificationColor)
                    .setLights(-0xff0001, 2000, 2000)
                    .setPriority(app.notifier.notificationPriority)
                    // channel, group, style
                    .setChannelId(app.notifier.notificationGroup)
                    .setGroup(app.notifier.notificationGroup)
                    .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(notification.text))
                    // intent, auto cancel
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
            if (!app.notifier.shouldBeQuiet()) {
                notificationBuilder.setDefaults(app.notifier.notificationDefaults)
            }
            app.notifier.notificationManager.notify(notification.id, notificationBuilder.build())
        }

        if (notificationList.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val intent = Intent(app, MainActivity::class.java)
            intent.action = "android.intent.action.MAIN"
            intent.putExtra("fragmentId", MainActivity.DRAWER_ITEM_NOTIFICATIONS)
            val pendingIntent = PendingIntent.getActivity(app, ID_NOTIFICATIONS,
                    intent, 0)

            val groupBuilder = NotificationCompat.Builder(app, app.notifier.notificationGroup)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setColor(app.notifier.notificationColor)
                    .setContentTitle(app.getString(R.string.notification_new_notification_title_format, unreadCount))
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .setChannelId(app.notifier.notificationGroup)
                    .setGroup(app.notifier.notificationGroup)
                    .setLights(-0xff0001, 2000, 2000)
                    .setPriority(app.notifier.notificationPriority)
                    .setContentIntent(pendingIntent)
                    .setStyle(NotificationCompat.BigTextStyle())
            if (!app.notifier.shouldBeQuiet()) {
                groupBuilder.setDefaults(app.notifier.notificationDefaults)
            }
            app.notifier.notificationManager.notify(ID_NOTIFICATIONS, groupBuilder.build())
        }

        app.db.notificationDao().setAllPosted()

        taskCallback.onCompleted()
    }
}

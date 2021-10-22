package pl.szczodrzynski.edziennik.data.api.task

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.util.SparseIntArray
import androidx.core.app.NotificationCompat
import androidx.core.util.forEach
import androidx.core.util.set
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.*
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.entity.Notification.Companion.TYPE_SERVER_MESSAGE
import pl.szczodrzynski.edziennik.ext.Intent
import pl.szczodrzynski.edziennik.ext.asBoldSpannable
import pl.szczodrzynski.edziennik.ext.concat
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.edziennik.data.db.entity.Notification as AppNotification

class PostNotifications(val app: App, nList: List<AppNotification>) {
    companion object {
        private const val TAG = "PostNotifications"
    }

    private val quiet by lazy { shouldBeQuiet() }
    fun shouldBeQuiet(): Boolean {
        if (!app.config.sync.quietHoursEnabled)
            return false
        val now = Time.getNow().value
        val start = app.config.sync.quietHoursStart?.value ?: return false
        var end = app.config.sync.quietHoursEnd?.value ?: return false
        if (start > end) {
            // the range spans between two days
            end += 240000
        }
        return now in start..end || now+240000 in start..end
    }

    private fun NotificationCompat.Builder.addDefaults(): NotificationCompat.Builder {
        return this.setColor(0xff2196f3.toInt())
                .setLights(0xff2196f3.toInt(), 2000, 2000)
                .setPriority(if (quiet) NotificationCompat.PRIORITY_LOW else NotificationCompat.PRIORITY_MAX)
                .also {
                    if (quiet) {
                        it.setSound(null)
                        it.setVibrate(longArrayOf())
                    }
                    else
                        it.setDefaults(NotificationCompat.DEFAULT_ALL)
                }
                .setGroup(if (quiet) app.notificationChannelsManager.dataQuiet.key else app.notificationChannelsManager.data.key)
    }

    private fun buildSummaryText(summaryCounts: SparseIntArray): CharSequence {
        val summaryTexts = mutableListOf<String>()
        summaryCounts.forEach { key, value ->
            if (value <= 0)
                return@forEach
            val pluralRes = when (key) {
                AppNotification.TYPE_TIMETABLE_LESSON_CHANGE -> R.plurals.notification_new_timetable_change_format
                AppNotification.TYPE_NEW_GRADE -> R.plurals.notification_new_grades_format
                AppNotification.TYPE_NEW_EVENT -> R.plurals.notification_new_events_format
                AppNotification.TYPE_NEW_HOMEWORK -> R.plurals.notification_new_homework_format
                AppNotification.TYPE_NEW_SHARED_EVENT -> R.plurals.notification_new_shared_events_format
                AppNotification.TYPE_NEW_SHARED_HOMEWORK -> R.plurals.notification_new_shared_homework_format
                AppNotification.TYPE_NEW_MESSAGE -> R.plurals.notification_new_messages_format
                AppNotification.TYPE_NEW_NOTICE -> R.plurals.notification_new_notices_format
                AppNotification.TYPE_NEW_ATTENDANCE -> R.plurals.notification_new_attendance_format
                AppNotification.TYPE_LUCKY_NUMBER -> R.plurals.notification_new_lucky_number_format
                AppNotification.TYPE_NEW_ANNOUNCEMENT -> R.plurals.notification_new_announcements_format
                else -> R.plurals.notification_other_format
            }
            summaryTexts += app.resources.getQuantityString(pluralRes, value, value)
        }
        return summaryTexts.concat(", ")
    }

    init { run {
        val count = nList.size
        if (count == 0)
            return@run
        val notificationManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val summaryCounts = SparseIntArray()

        val newNotificationsText = app.resources.getQuantityString(R.plurals.notification_count_format, count, count)
        val newNotificationsShortText = app.resources.getQuantityString(R.plurals.notification_count_short_format, count, count)

        val intent = Intent(
                app,
                MainActivity::class.java,
                "fragmentId" to MainActivity.DRAWER_ITEM_NOTIFICATIONS
        )
        val summaryIntent = PendingIntent.getActivity(app, app.notificationChannelsManager.data.id, intent, PendingIntent.FLAG_ONE_SHOT)

        // On Nougat or newer - show maximum 8 notifications
        // On Marshmallow or older - show maximum 4 notifications

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N && count > 4 || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && count > 8) {
            val summaryList = mutableListOf<CharSequence>()
            nList.forEach {
                summaryCounts[it.type]++
                summaryList += listOf(
                        it.profileName.asBoldSpannable(),
                        it.text
                ).concat(": ")
            }

            // Create a summary to show *instead* of notifications
            val combined = NotificationCompat.Builder(app, app.notificationChannelsManager.data.key)
                    .setContentTitle(app.getString(R.string.app_name))
                    .setContentText(buildSummaryText(summaryCounts))
                    .setTicker(newNotificationsText)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setLargeIcon(IconicsDrawable(app).apply {
                        icon = CommunityMaterial.Icon.cmd_bell_ring_outline
                        colorRes = R.color.colorPrimary
                    }.toBitmap())
                    .setStyle(NotificationCompat.InboxStyle()
                            .also {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    it.setBigContentTitle(app.getString(R.string.app_name))
                                    it.setSummaryText(newNotificationsShortText)
                                }
                                else {
                                    it.setBigContentTitle(newNotificationsText)
                                    it.setSummaryText(app.getString(R.string.notification_click_to_see_all))
                                }
                                summaryList.forEach { line ->
                                    it.addLine(line)
                                }
                            })
                    .addDefaults()
                    .setContentIntent(summaryIntent)
                    .setAutoCancel(true)
                    .build()
            notificationManager.notify(System.currentTimeMillis().toInt(), combined)
        }
        else {
            // Less than 8 notifications
            val notifications = nList.map {
                summaryCounts[it.type]++
                NotificationCompat.Builder(app, app.notificationChannelsManager.data.key)
                        .setContentTitle(it.profileName ?: app.getString(R.string.app_name))
                        .setContentText(it.text)
                        .setSubText(if (it.type == TYPE_SERVER_MESSAGE) null else it.title)
                        .setTicker("${it.profileName}: ${it.title}")
                        .setSmallIcon(R.drawable.ic_notification)
                        .setLargeIcon(IconicsDrawable(app, it.getLargeIcon()).apply {
                            colorRes = R.color.colorPrimary
                        }.toBitmap())
                        .setStyle(NotificationCompat.BigTextStyle()
                                .bigText(it.textLong ?: it.text))
                        .setWhen(it.addedDate)
                        .addDefaults()
                        .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_SUMMARY)
                        .setContentIntent(it.getPendingIntent(app))
                        .setAutoCancel(true)
                        .build()
            }

            val time = System.currentTimeMillis()
            notificationManager.apply {
                notifications.forEachIndexed { index, it ->
                    notificationManager.notify((time + index).toInt(), it)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val summary = NotificationCompat.Builder(app, app.notificationChannelsManager.data.key)
                        .setContentTitle(newNotificationsText)
                        .setContentText(buildSummaryText(summaryCounts))
                        .setTicker(newNotificationsText)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setLargeIcon(IconicsDrawable(app).apply {
                            icon = CommunityMaterial.Icon.cmd_bell_ring_outline
                            colorRes = R.color.colorPrimary
                        }.toBitmap())
                        .addDefaults()
                        .setGroupSummary(true)
                        .setContentIntent(summaryIntent)
                        .setAutoCancel(true)
                        .build()

                notificationManager.notify(app.notificationChannelsManager.data.id, summary)
            }
        }
    }}
}

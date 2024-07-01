/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-15.
 */

package pl.szczodrzynski.edziennik.core.manager

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat.*
import androidx.core.app.NotificationManagerCompat.*
import pl.szczodrzynski.edziennik.R

class NotificationManager(val c: Context) {
    data class Channel(
            val id: Int,
            val key: String,
            val name: String,
            val description: String,
            val importance: Int,
            val priority: Int,
            val quiet: Boolean = false,
            val lightColor: Int? = null
    )

    fun registerAllChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
            return
        val manager = c.getSystemService(NotificationManager::class.java)

        val registered = manager.notificationChannels.map { it.id }.toSet()
        val all = all.map { it.key }.toSet()

        val toRegister = all - registered
        val toDelete = registered - all

        for (key in toRegister) {
            val channel = this.all.firstOrNull { it.key == key } ?: continue
            manager.createNotificationChannel(NotificationChannel(key, channel.name, channel.importance).apply {
                description = channel.description
                if (channel.quiet) {
                    enableVibration(false)
                    setSound(null, null)
                }
                channel.lightColor?.let {
                    enableLights(true)
                    lightColor = it
                }
            })
        }

        for (key in toDelete) {
            if (key.contains("chucker"))
                continue
            manager.deleteNotificationChannel(key)
        }
    }

    val sync = Channel(
            1,
            "pl.szczodrzynski.edziennik.SYNC",
            c.getString(R.string.notification_channel_get_data_name),
            c.getString(R.string.notification_channel_get_data_desc),
            IMPORTANCE_MIN,
            PRIORITY_MIN,
            quiet = true
    )

    val data = Channel(
            50,
            "pl.szczodrzynski.edziennik.DATA",
            c.getString(R.string.notification_channel_notifications_name),
            c.getString(R.string.notification_channel_notifications_desc),
            IMPORTANCE_HIGH,
            PRIORITY_MAX,
            lightColor = 0xff2196f3.toInt()
    )

    val dataQuiet = Channel(
            60,
            "pl.szczodrzynski.edziennik.DATA_QUIET",
            c.getString(R.string.notification_channel_notifications_quiet_name),
            c.getString(R.string.notification_channel_notifications_quiet_desc),
            IMPORTANCE_LOW,
            PRIORITY_LOW,
            quiet = true
    )

    val updates = Channel(
            100,
            "pl.szczodrzynski.edziennik.UPDATES",
            c.getString(R.string.notification_channel_updates_name),
            c.getString(R.string.notification_channel_updates_desc),
            IMPORTANCE_DEFAULT,
            PRIORITY_DEFAULT
    )

    val userAttention = Channel(
            200,
            "pl.szczodrzynski.edziennik.USER_ATTENTION",
            c.getString(R.string.notification_channel_user_attention_name),
            c.getString(R.string.notification_channel_user_attention_desc),
            IMPORTANCE_DEFAULT,
            PRIORITY_DEFAULT
    )

    val all by lazy { listOf(
            sync,
            data,
            dataQuiet,
            updates,
            userAttention
    ) }
}

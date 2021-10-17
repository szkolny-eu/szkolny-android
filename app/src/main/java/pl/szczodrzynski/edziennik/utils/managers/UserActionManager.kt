/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-15.
 */

package pl.szczodrzynski.edziennik.utils.managers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import org.greenrobot.eventbus.EventBus
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.ERROR_CAPTCHA_LIBRUS_PORTAL
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.events.UserActionRequiredEvent
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.ui.captcha.LibrusCaptchaDialog

class UserActionManager(val app: App) {
    companion object {
        private const val TAG = "UserActionManager"
    }

    fun requiresUserAction(apiError: ApiError): Boolean {
        return apiError.errorCode == ERROR_CAPTCHA_LIBRUS_PORTAL
    }

    fun sendToUser(apiError: ApiError) {
        val type = when (apiError.errorCode) {
            ERROR_CAPTCHA_LIBRUS_PORTAL -> UserActionRequiredEvent.CAPTCHA_LIBRUS
            else -> 0
        }

        if (EventBus.getDefault().hasSubscriberForEvent(UserActionRequiredEvent::class.java)) {
            EventBus.getDefault().post(UserActionRequiredEvent(apiError.profileId ?: -1, type))
            return
        }

        val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val text = app.getString(when (type) {
            UserActionRequiredEvent.CAPTCHA_LIBRUS -> R.string.notification_user_action_required_captcha_librus
            else -> R.string.notification_user_action_required_text
        }, apiError.profileId)

        val intent = Intent(
                app,
                MainActivity::class.java,
                "action" to "userActionRequired",
                "profileId" to (apiError.profileId ?: -1),
                "type" to type
        )
        val pendingIntent = PendingIntent.getActivity(app, System.currentTimeMillis().toInt(), intent, PendingIntent.FLAG_ONE_SHOT)

        val notification = NotificationCompat.Builder(app, app.notificationChannelsManager.userAttention.key)
                .setContentTitle(app.getString(R.string.notification_user_action_required_title))
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_error_outline)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setColor(0xff2196f3.toInt())
                .setLights(0xff2196f3.toInt(), 2000, 2000)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    fun execute(
            activity: AppCompatActivity,
            profileId: Int?,
            type: Int,
            onSuccess: ((code: String) -> Unit)? = null,
            onFailure: (() -> Unit)? = null
    ) {
        if (type != UserActionRequiredEvent.CAPTCHA_LIBRUS)
            return

        if (profileId == null)
            return
        // show captcha dialog
        // use passed onSuccess listener, else sync profile
        LibrusCaptchaDialog(activity, onSuccess = onSuccess ?: { code ->
            EdziennikTask.syncProfile(profileId, arguments = JsonObject(
                    "recaptchaCode" to code,
                    "recaptchaTime" to System.currentTimeMillis()
            )).enqueue(activity)
        }, onFailure = onFailure)
    }
}

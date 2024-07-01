/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-15.
 */

package pl.szczodrzynski.edziennik.core.manager

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.events.UserActionRequiredEvent
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.ui.captcha.RecaptchaPromptDialog
import pl.szczodrzynski.edziennik.ui.login.oauth.OAuthLoginActivity
import pl.szczodrzynski.edziennik.ui.login.oauth.OAuthLoginResult
import pl.szczodrzynski.edziennik.ui.login.recaptcha.RecaptchaActivity
import pl.szczodrzynski.edziennik.ui.login.recaptcha.RecaptchaResult
import pl.szczodrzynski.edziennik.utils.Utils.d

class UserActionManager(val app: App) {
    companion object {
        private const val TAG = "UserActionManager"
    }

    fun sendToUser(event: UserActionRequiredEvent) {
        if (EventBus.getDefault().hasSubscriberForEvent(UserActionRequiredEvent::class.java)) {
            EventBus.getDefault().post(event)
            return
        }

        val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val text = app.getString(event.errorText, event.profileId)
        val intent = Intent(
            app,
            MainActivity::class.java,
            "action" to "userActionRequired",
            "profileId" to event.profileId,
            "type" to event.type,
            "params" to event.params,
        )
        val pendingIntent = PendingIntent.getActivity(
            app,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_ONE_SHOT or pendingIntentFlag(),
        )

        val notification =
            NotificationCompat.Builder(app, app.notificationChannelsManager.userAttention.key)
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

    class UserActionCallback(
        val onSuccess: ((data: Bundle) -> Unit)? = null,
        val onFailure: (() -> Unit)? = null,
        val onCancel: (() -> Unit)? = null,
    )

    fun execute(
        activity: AppCompatActivity,
        event: UserActionRequiredEvent,
        callback: UserActionCallback,
    ) {
        d(TAG, "Running user action (${event.type}) with params: ${event.params}")
        val isSuccessful = when (event.type) {
            UserActionRequiredEvent.Type.RECAPTCHA -> executeRecaptcha(activity, event, callback)
            UserActionRequiredEvent.Type.OAUTH -> executeOauth(activity, event, callback)
        }
        if (!isSuccessful)
            callback.onFailure?.invoke()
    }

    private fun executeRecaptcha(
        activity: AppCompatActivity,
        event: UserActionRequiredEvent,
        callback: UserActionCallback,
    ): Boolean {
        val siteKey = event.params.getString("siteKey") ?: return false
        val referer = event.params.getString("referer") ?: return false
        RecaptchaPromptDialog(
            activity = activity,
            siteKey = siteKey,
            referer = referer,
            onSuccess = { code ->
                finishAction(activity, event, callback, Bundle(
                    "recaptchaCode" to code,
                    "recaptchaTime" to System.currentTimeMillis(),
                ))
            },
            onCancel = callback.onCancel,
            onServerError = {
                executeRecaptchaActivity(activity, event, callback)
            },
        ).show()
        return true
    }

    private fun executeRecaptchaActivity(
        activity: AppCompatActivity,
        event: UserActionRequiredEvent,
        callback: UserActionCallback,
    ): Boolean {
        event.params.getString("siteKey") ?: return false
        event.params.getString("referer") ?: return false

        var listener: Any? = null
        listener = object {
            @Subscribe(threadMode = ThreadMode.MAIN)
            fun onRecaptchaResult(result: RecaptchaResult) {
                EventBus.getDefault().unregister(listener)
                when {
                    result.isError -> callback.onFailure?.invoke()
                    result.code != null -> {
                        finishAction(activity, event, callback, Bundle(
                            "recaptchaCode" to result.code,
                            "recaptchaTime" to System.currentTimeMillis(),
                        ))
                    }
                    else -> callback.onCancel?.invoke()
                }
            }
        }
        EventBus.getDefault().register(listener)

        val intent = Intent(activity, RecaptchaActivity::class.java).putExtras(event.params)
        activity.startActivity(intent)
        return true
    }

    private fun executeOauth(
        activity: AppCompatActivity,
        event: UserActionRequiredEvent,
        callback: UserActionCallback,
    ): Boolean {
        val storeKey = event.params.getString("responseStoreKey") ?: return false
        event.params.getString("authorizeUrl") ?: return false
        event.params.getString("redirectUrl") ?: return false

        var listener: Any? = null
        listener = object {
            @Subscribe(threadMode = ThreadMode.MAIN)
            fun onOAuthLoginResult(result: OAuthLoginResult) {
                EventBus.getDefault().unregister(listener)
                when {
                    result.isError -> callback.onFailure?.invoke()
                    result.responseUrl != null -> {
                        finishAction(activity, event, callback, Bundle(
                            storeKey to result.responseUrl,
                        ))
                    }
                    else -> callback.onCancel?.invoke()
                }
            }
        }
        EventBus.getDefault().register(listener)

        val intent = Intent(activity, OAuthLoginActivity::class.java).putExtras(event.params)
        activity.startActivity(intent)
        return true
    }

    private fun finishAction(
        activity: AppCompatActivity,
        event: UserActionRequiredEvent,
        callback: UserActionCallback,
        data: Bundle,
    ) {
        val extras = event.params.getBundle("extras")
        if (extras != null)
            data.putAll(extras)

        if (callback.onSuccess != null)
            callback.onSuccess.invoke(data)
        else if (event.profileId != null)
            EdziennikTask.syncProfile(
                profileId = event.profileId,
                arguments = data.toJsonObject(),
            ).enqueue(activity)
        else
            callback.onFailure?.invoke()
    }
}

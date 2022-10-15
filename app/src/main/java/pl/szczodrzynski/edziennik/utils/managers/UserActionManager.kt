/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-15.
 */

package pl.szczodrzynski.edziennik.utils.managers

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
import pl.szczodrzynski.edziennik.data.api.ERROR_CAPTCHA_LIBRUS_PORTAL
import pl.szczodrzynski.edziennik.data.api.ERROR_USOS_OAUTH_LOGIN_REQUEST
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.events.UserActionRequiredEvent
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.ui.captcha.LibrusCaptchaDialog
import pl.szczodrzynski.edziennik.ui.login.oauth.OAuthLoginActivity
import pl.szczodrzynski.edziennik.ui.login.oauth.OAuthLoginResult
import pl.szczodrzynski.edziennik.utils.Utils.d

class UserActionManager(val app: App) {
    companion object {
        private const val TAG = "UserActionManager"
    }

    fun requiresUserAction(apiError: ApiError) = when (apiError.errorCode) {
        ERROR_CAPTCHA_LIBRUS_PORTAL -> true
        ERROR_USOS_OAUTH_LOGIN_REQUEST -> true
        else -> false
    }

    fun sendToUser(apiError: ApiError) {
        val type = when (apiError.errorCode) {
            ERROR_CAPTCHA_LIBRUS_PORTAL -> UserActionRequiredEvent.CAPTCHA_LIBRUS
            ERROR_USOS_OAUTH_LOGIN_REQUEST -> UserActionRequiredEvent.OAUTH_USOS
            else -> 0
        }

        if (EventBus.getDefault().hasSubscriberForEvent(UserActionRequiredEvent::class.java)) {
            EventBus.getDefault().post(UserActionRequiredEvent(apiError.profileId ?: -1, type, apiError.params))
            return
        }

        val manager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val text = app.getString(when (type) {
            UserActionRequiredEvent.CAPTCHA_LIBRUS -> R.string.notification_user_action_required_captcha_librus
            UserActionRequiredEvent.OAUTH_USOS -> R.string.notification_user_action_required_oauth_usos
            else -> R.string.notification_user_action_required_text
        }, apiError.profileId)

        val intent = Intent(
                app,
                MainActivity::class.java,
                "action" to "userActionRequired",
                "profileId" to (apiError.profileId ?: -1),
                "type" to type,
                "params" to apiError.params,
        )
        val pendingIntent = PendingIntent.getActivity(app, System.currentTimeMillis().toInt(), intent, PendingIntent.FLAG_ONE_SHOT or pendingIntentFlag())

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
            params: Bundle? = null,
            onSuccess: ((params: Bundle) -> Unit)? = null,
            onFailure: (() -> Unit)? = null
    ) {
        d(TAG, "Running user action ($type) with params: ${params?.toJsonObject()}")
        val isSuccessful = when (type) {
            UserActionRequiredEvent.CAPTCHA_LIBRUS -> executeLibrus(activity, profileId, params, onSuccess, onFailure)
            UserActionRequiredEvent.OAUTH_USOS -> executeOauth(activity, profileId, params, onSuccess, onFailure)
            else -> false
        }
        if (!isSuccessful) {
            onFailure?.invoke()
        }
    }

    private fun executeLibrus(
        activity: AppCompatActivity,
        profileId: Int?,
        params: Bundle?,
        onSuccess: ((params: Bundle) -> Unit)?,
        onFailure: (() -> Unit)?,
    ): Boolean {
        if (profileId == null)
            return false
        val extras = params?.getBundle("extras")
        // show captcha dialog
        // use passed onSuccess listener, else sync profile
        LibrusCaptchaDialog(
            activity = activity,
            onSuccess = { code ->
                val args = Bundle(
                    "recaptchaCode" to code,
                    "recaptchaTime" to System.currentTimeMillis(),
                )
                if (extras != null)
                    args.putAll(extras)

                if (onSuccess != null)
                    onSuccess(args)
                else
                    EdziennikTask.syncProfile(profileId, arguments = args.toJsonObject()).enqueue(activity)
            },
            onFailure = onFailure,
        ).show()
        return true
    }

    private fun executeOauth(
        activity: AppCompatActivity,
        profileId: Int?,
        params: Bundle?,
        onSuccess: ((params: Bundle) -> Unit)?,
        onFailure: (() -> Unit)?,
    ): Boolean {
        if (profileId == null || params == null)
            return false
        val extras = params.getBundle("extras")
        val storeKey = params.getString("responseStoreKey") ?: return false
        params.getString("authorizeUrl") ?: return false
        params.getString("redirectUrl") ?: return false

        var listener: Any? = null
        listener = object {
            @Subscribe(threadMode = ThreadMode.MAIN)
            fun onOAuthLoginResult(result: OAuthLoginResult) {
                EventBus.getDefault().unregister(listener)
                when {
                    result.isError -> onFailure?.invoke()
                    result.responseUrl != null -> {
                        val args = Bundle(
                            storeKey to result.responseUrl,
                        )
                        if (extras != null)
                            args.putAll(extras)

                        if (onSuccess != null)
                            onSuccess(args)
                        else
                            EdziennikTask.syncProfile(profileId, arguments = args.toJsonObject()).enqueue(activity)
                    }
                }
            }
        }
        EventBus.getDefault().register(listener)

        val intent = Intent(activity, OAuthLoginActivity::class.java).putExtras(params)
        activity.startActivity(intent)
        return true
    }
}

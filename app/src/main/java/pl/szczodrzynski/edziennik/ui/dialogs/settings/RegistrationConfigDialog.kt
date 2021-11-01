/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-15.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.data.api.task.AppSync
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.utils.html.BetterHtml
import kotlin.coroutines.CoroutineContext

class RegistrationConfigDialog(
    val activity: AppCompatActivity,
    val profile: Profile,
    val onChangeListener: (suspend (enabled: Boolean) -> Unit)? = null,
    val onShowListener: ((tag: String) -> Unit)? = null,
    val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {
    companion object {
        private const val TAG = "RegistrationEnableDialog"
    }

    private lateinit var app: App
    private lateinit var dialog: AlertDialog

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local variables go here

    init { run {
        if (activity.isFinishing)
            return@run
        app = activity.applicationContext as App
    }}

    fun showEventShareDialog() {
        onShowListener?.invoke(TAG + "EventShare")
        dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.registration_config_event_sharing_title)
            .setMessage(R.string.registration_config_event_sharing_text)
            .setPositiveButton(R.string.i_agree) { _, _ ->
                enableRegistration()
            }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener {
                onDismissListener?.invoke(TAG + "EventShare")
            }
            .show()
    }

    fun showNoteShareDialog() {
        onShowListener?.invoke(TAG + "NoteShare")
        dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.registration_config_note_sharing_title)
            .setMessage(R.string.registration_config_note_sharing_text)
            .setPositiveButton(R.string.i_agree) { _, _ ->
                enableRegistration()
            }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener {
                onDismissListener?.invoke(TAG + "NoteShare")
            }
            .show()
    }

    fun showEnableDialog() {
        onShowListener?.invoke(TAG + "Enable")
        dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.registration_config_title)
            .setMessage(BetterHtml.fromHtml(activity, R.string.registration_config_enable_text))
            .setPositiveButton(R.string.i_agree) { _, _ ->
                enableRegistration()
            }
            .setNegativeButton(R.string.i_disagree, null)
            .setOnDismissListener {
                onDismissListener?.invoke(TAG + "Enable")
            }
            .show()
    }

    fun showDisableDialog() {
        onShowListener?.invoke(TAG + "Disable")
        dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.registration_config_title)
            .setMessage(R.string.registration_config_disable_text)
            .setPositiveButton(R.string.ok) { _, _ ->
                disableRegistration()
            }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener {
                onDismissListener?.invoke(TAG + "Disable")
            }
            .show()
    }

    private fun enableRegistration() = launch {
        onShowListener?.invoke(TAG + "Enabling")
        dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.please_wait)
            .setMessage(R.string.registration_config_enable_progress_text)
            .setCancelable(false)
            .setOnDismissListener {
                onDismissListener?.invoke(TAG + "Enabling")
            }
            .show()

        withContext(Dispatchers.Default) {
            profile.registration = Profile.REGISTRATION_ENABLED

            // force full registration of the user
            App.config.getFor(profile.id).hash = ""

            SzkolnyApi(app).runCatching(activity) {
                AppSync(app, mutableListOf(), listOf(profile), this).run(
                    0L,
                    markAsSeen = true
                )
            }
            app.db.profileDao().add(profile)
            if (profile.id == App.profileId) {
                App.profile.registration = profile.registration
            }
        }

        dialog.dismiss()
        onChangeListener?.invoke(true)
    }

    private fun disableRegistration() = launch {
        onShowListener?.invoke(TAG + "Disabling")
        dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.please_wait)
            .setMessage(R.string.registration_config_disable_progress_text)
            .setCancelable(false)
            .setOnDismissListener {
                onDismissListener?.invoke(TAG + "Disabling")
            }
            .show()

        withContext(Dispatchers.Default) {
            profile.registration = Profile.REGISTRATION_DISABLED

            SzkolnyApi(app).runCatching(activity) {
                unregisterAppUser(profile.userCode)
            }
            app.db.profileDao().add(profile)
            if (profile.id == App.profileId) {
                App.profile.registration = profile.registration
            }
        }

        dialog.dismiss()
        onChangeListener?.invoke(false)
    }
}

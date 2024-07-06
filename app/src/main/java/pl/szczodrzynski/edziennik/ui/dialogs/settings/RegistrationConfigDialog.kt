/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-15.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.data.api.task.AppSync
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.ui.base.dialog.BaseDialog
import pl.szczodrzynski.edziennik.ui.base.dialog.SimpleDialog
import pl.szczodrzynski.edziennik.utils.html.BetterHtml
import kotlin.coroutines.CoroutineContext

class RegistrationConfigDialog(
    val activity: AppCompatActivity,
    val profile: Profile,
    val onChangeListener: (suspend (enabled: Boolean) -> Unit)? = null,
    val onShowListener: ((tag: String) -> Unit)? = null,
    val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {

    private lateinit var app: App
    private lateinit var dialog: BaseDialog<*>

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
        dialog = SimpleDialog<Unit>(activity, onShowListener, onDismissListener) {
            title(R.string.registration_config_event_sharing_title)
            message(R.string.registration_config_event_sharing_text)
            positive(R.string.i_agree) {
                enableRegistration()
            }
            negative(R.string.cancel)
        }.show()
    }

    fun showNoteShareDialog() {
        dialog = SimpleDialog<Unit>(activity, onShowListener, onDismissListener) {
            title(R.string.registration_config_note_sharing_title)
            message(R.string.registration_config_note_sharing_text)
            positive(R.string.i_agree) {
                enableRegistration()
            }
            negative(R.string.cancel)
        }.show()
    }

    fun showEnableDialog() {
        dialog = SimpleDialog<Unit>(activity, onShowListener, onDismissListener) {
            title(R.string.registration_config_title)
            message(BetterHtml.fromHtml(activity, R.string.registration_config_enable_text))
            positive(R.string.i_agree) {
                enableRegistration()
            }
            negative(R.string.i_disagree)
        }.show()
    }

    fun showDisableDialog() {
        dialog = SimpleDialog<Unit>(activity, onShowListener, onDismissListener) {
            title(R.string.registration_config_title)
            message(R.string.registration_config_disable_text)
            positive(R.string.ok) {
                disableRegistration()
            }
            negative(R.string.cancel)
        }.show()
    }

    private fun enableRegistration() = launch {
        dialog = SimpleDialog<Unit>(activity, onShowListener, onDismissListener) {
            title(R.string.please_wait)
            message(R.string.registration_config_enable_progress_text)
            cancelable(false)
        }.show()

        withContext(Dispatchers.Default) {
            profile.registration = Profile.REGISTRATION_ENABLED

            // force full registration of the user
            profile.config.hash = ""

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
        dialog = SimpleDialog<Unit>(activity, onShowListener, onDismissListener) {
            title(R.string.please_wait)
            message(R.string.registration_config_disable_progress_text)
            cancelable(false)
        }.show()

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

/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-15.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.sync

import android.text.Html
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.data.api.task.AppSync
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import kotlin.coroutines.CoroutineContext

class RegistrationEnableDialog(
        val activity: AppCompatActivity,
        val profileId: Int
) : CoroutineScope {
    companion object {
        private const val TAG = "RegistrationEnableDialog"
    }

    private lateinit var app: App

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local variables go here
    private var progressDialog: AlertDialog? = null

    init { run {
        if (activity.isFinishing)
            return@run
        app = activity.applicationContext as App
    }}

    fun showEventShareDialog(onSuccess: (profile: Profile?) -> Unit) {
        MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.event_manual_need_registration_title)
                .setMessage(R.string.event_manual_need_registration_text)
                .setPositiveButton(R.string.ok) { dialog, which ->
                    enableRegistration(onSuccess)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    fun showEnableDialog(onSuccess: (profile: Profile?) -> Unit) {
        MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.registration_enable_dialog_title)
                .setMessage(Html.fromHtml(app.getString(R.string.registration_enable_dialog_text)))
                .setPositiveButton(R.string.ok) { dialog, which ->
                    enableRegistration(onSuccess)
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
    }

    private fun enableRegistration(onSuccess: (profile: Profile?) -> Unit) { launch {
        progressDialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.please_wait)
                .setMessage(R.string.registration_enable_progress_text)
                .setCancelable(false)
                .show()

        val profile = withContext(Dispatchers.Default) {
            val profile = app.db.profileDao().getByIdNow(profileId) ?: return@withContext null
            profile.registration = Profile.REGISTRATION_ENABLED

            // force full registration of the user
            App.config.getFor(profile.id).hash = ""

            AppSync(app, mutableListOf(), listOf(profile), SzkolnyApi(app)).run(0L, markAsSeen = true)
            app.db.profileDao().add(profile)
            if (profile.id == App.profileId) {
                App.profile.registration = profile.registration
            }
            return@withContext profile
        }

        progressDialog?.dismiss()
        onSuccess(profile)
    }}
}

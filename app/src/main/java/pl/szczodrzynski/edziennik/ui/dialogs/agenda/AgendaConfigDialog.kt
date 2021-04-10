/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-10.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.agenda

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.entity.Profile.Companion.REGISTRATION_ENABLED
import pl.szczodrzynski.edziennik.databinding.DialogConfigAgendaBinding
import pl.szczodrzynski.edziennik.ui.dialogs.sync.RegistrationConfigDialog
import java.util.*

class AgendaConfigDialog(
    private val activity: AppCompatActivity,
    private val reloadOnDismiss: Boolean = true,
    private val onShowListener: ((tag: String) -> Unit)? = null,
    private val onDismissListener: ((tag: String) -> Unit)? = null
) {
    companion object {
        const val TAG = "AgendaConfigDialog"
    }

    private val app by lazy { activity.application as App }
    private val config by lazy { app.config.ui }
    private val profileConfig by lazy { app.config.forProfile().ui }

    private lateinit var b: DialogConfigAgendaBinding
    private lateinit var dialog: AlertDialog

    init { run {
        if (activity.isFinishing)
            return@run
        b = DialogConfigAgendaBinding.inflate(activity.layoutInflater)
        onShowListener?.invoke(TAG)
        dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.menu_agenda_config)
            .setView(b.root)
            .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener {
                saveConfig()
                onDismissListener?.invoke(TAG)
                if (reloadOnDismiss) (activity as? MainActivity)?.reloadTarget()
            }
            .create()
        loadConfig()
        dialog.show()
    }}

    private fun loadConfig() {
        b.config = profileConfig
        b.isAgendaMode = profileConfig.agendaViewType == Profile.AGENDA_DEFAULT

        b.eventSharingEnabled.isChecked = app.profile.enableSharedEvents
                && app.profile.registration == REGISTRATION_ENABLED
        b.eventSharingEnabled.onChange { _, isChecked ->
            if (isChecked && app.profile.registration != REGISTRATION_ENABLED) {
                b.eventSharingEnabled.isChecked = false
                val dialog = RegistrationConfigDialog(activity, app.profile, onChangeListener = { enabled ->
                    b.eventSharingEnabled.isChecked = enabled
                    setEventSharingEnabled(enabled)
                }, onShowListener, onDismissListener)
                dialog.showEnableDialog()
                return@onChange
            }
            setEventSharingEnabled(isChecked)
        }
    }

    private fun setEventSharingEnabled(enabled: Boolean) {
        if (enabled == app.profile.enableSharedEvents)
            return
        app.profile.enableSharedEvents = enabled
        app.profileSave()
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.event_sharing)
            .setMessage(
                if (enabled)
                    R.string.settings_register_shared_events_dialog_enabled_text
                else
                    R.string.settings_register_shared_events_dialog_disabled_text
            )
            .setPositiveButton(R.string.ok, null)
            .show()
    }

    private fun saveConfig() {

    }
}

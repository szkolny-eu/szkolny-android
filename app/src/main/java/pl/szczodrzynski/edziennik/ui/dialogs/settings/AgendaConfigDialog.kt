/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-10.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.entity.Profile.Companion.REGISTRATION_ENABLED
import pl.szczodrzynski.edziennik.databinding.DialogConfigAgendaBinding
import pl.szczodrzynski.edziennik.ext.onChange
import pl.szczodrzynski.edziennik.ui.dialogs.base.ConfigDialog
import java.util.*

class AgendaConfigDialog(
    activity: AppCompatActivity,
    reloadOnDismiss: Boolean = true,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : ConfigDialog<DialogConfigAgendaBinding>(
    activity,
    reloadOnDismiss,
    onShowListener,
    onDismissListener,
) {

    override val TAG = "AgendaConfigDialog"

    override fun getTitleRes() = R.string.menu_agenda_config
    override fun inflate(layoutInflater: LayoutInflater) =
        DialogConfigAgendaBinding.inflate(layoutInflater)

    private val profileConfig by lazy { app.config.forProfile().ui }

    override suspend fun loadConfig() {
        b.config = profileConfig
        b.isAgendaMode = profileConfig.agendaViewType == Profile.AGENDA_DEFAULT

        b.eventSharingEnabled.isChecked =
            app.profile.enableSharedEvents && app.profile.registration == REGISTRATION_ENABLED
        b.eventSharingEnabled.onChange { _, isChecked ->
            if (isChecked && app.profile.registration != REGISTRATION_ENABLED) {
                b.eventSharingEnabled.isChecked = false
                val dialog = RegistrationConfigDialog(
                    activity,
                    app.profile,
                    onChangeListener = { enabled ->
                        b.eventSharingEnabled.isChecked = enabled
                        setEventSharingEnabled(enabled)
                    },
                    onShowListener,
                    onDismissListener,
                )
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
}

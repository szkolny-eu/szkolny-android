/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-10.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.databinding.DialogConfigAgendaBinding
import pl.szczodrzynski.edziennik.ext.onChange
import pl.szczodrzynski.edziennik.ui.base.dialog.ConfigDialog

class AgendaConfigDialog(
    activity: AppCompatActivity,
    reloadOnDismiss: Boolean = true,
) : ConfigDialog<DialogConfigAgendaBinding>(activity, reloadOnDismiss) {

    override fun getTitleRes() = R.string.menu_agenda_config
    override fun inflate(layoutInflater: LayoutInflater) =
        DialogConfigAgendaBinding.inflate(layoutInflater)

    override suspend fun loadConfig() {
        b.config = app.profile.config
        b.isAgendaMode = app.profile.config.ui.agendaViewType == Profile.AGENDA_DEFAULT

        var calledFromListener = false
        b.eventSharingEnabled.isChecked = app.profile.canShare
        b.shareByDefault.isEnabled = app.profile.canShare
        b.eventSharingEnabled.onChange { _, isChecked ->
            if (calledFromListener) {
                calledFromListener = false
                return@onChange
            }
            b.eventSharingEnabled.isChecked = !isChecked
            val dialog = RegistrationConfigDialog(
                activity,
                app.profile,
                onChangeListener = { enabled ->
                    calledFromListener = true
                    b.eventSharingEnabled.isChecked = enabled
                    b.shareByDefault.isEnabled = enabled
                },
            )
            if (isChecked)
                dialog.showEnableDialog()
            else
                dialog.showDisableDialog()
            return@onChange
        }
    }
}

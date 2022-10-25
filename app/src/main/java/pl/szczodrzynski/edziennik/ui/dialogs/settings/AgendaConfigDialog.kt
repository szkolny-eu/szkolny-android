/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-10.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.entity.Profile
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

    private val profileConfig by lazy { app.config.forProfile() }

    override suspend fun loadConfig() {
        b.config = profileConfig
        b.isAgendaMode = profileConfig.ui.agendaViewType == Profile.AGENDA_DEFAULT

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
                onShowListener,
                onDismissListener,
            )
            if (isChecked)
                dialog.showEnableDialog()
            else
                dialog.showDisableDialog()
            return@onChange
        }
    }
}

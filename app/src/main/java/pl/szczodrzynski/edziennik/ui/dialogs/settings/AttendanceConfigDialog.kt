/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.AttendanceConfigDialogBinding
import pl.szczodrzynski.edziennik.ext.onChange
import pl.szczodrzynski.edziennik.ui.dialogs.base.ConfigDialog

class AttendanceConfigDialog(
    activity: AppCompatActivity,
    reloadOnDismiss: Boolean = true,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : ConfigDialog<AttendanceConfigDialogBinding>(
    activity,
    reloadOnDismiss,
    onShowListener,
    onDismissListener,
) {

    override val TAG = "AttendanceConfigDialog"

    override fun getTitleRes() = R.string.menu_attendance_config
    override fun inflate(layoutInflater: LayoutInflater) =
        AttendanceConfigDialogBinding.inflate(layoutInflater)

    private val profileConfig by lazy { app.config.getFor(app.profileId).attendance }

    override suspend fun loadConfig() {
        b.useSymbols.isChecked = profileConfig.useSymbols
        b.groupConsecutiveDays.isChecked = profileConfig.groupConsecutiveDays
        b.showPresenceInMonth.isChecked = profileConfig.showPresenceInMonth
    }

    override fun initView() {
        b.useSymbols.onChange { _, isChecked ->
            profileConfig.useSymbols = isChecked
        }
        b.groupConsecutiveDays.onChange { _, isChecked ->
            profileConfig.groupConsecutiveDays = isChecked
        }
        b.showPresenceInMonth.onChange { _, isChecked ->
            profileConfig.showPresenceInMonth = isChecked
        }
    }
}

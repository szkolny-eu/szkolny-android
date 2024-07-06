/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.AttendanceConfigDialogBinding
import pl.szczodrzynski.edziennik.ext.onChange
import pl.szczodrzynski.edziennik.ui.base.dialog.ConfigDialog

class AttendanceConfigDialog(
    activity: AppCompatActivity,
    reloadOnDismiss: Boolean = true,
) : ConfigDialog<AttendanceConfigDialogBinding>(activity, reloadOnDismiss) {

    override fun getTitleRes() = R.string.menu_attendance_config
    override fun inflate(layoutInflater: LayoutInflater) =
        AttendanceConfigDialogBinding.inflate(layoutInflater)

    override suspend fun loadConfig() {
        b.useSymbols.isChecked = app.profile.config.attendance.useSymbols
        b.groupConsecutiveDays.isChecked = app.profile.config.attendance.groupConsecutiveDays
        b.showPresenceInMonth.isChecked = app.profile.config.attendance.showPresenceInMonth
    }

    override fun initView() {
        b.useSymbols.onChange { _, isChecked ->
            app.profile.config.attendance.useSymbols = isChecked
        }
        b.groupConsecutiveDays.onChange { _, isChecked ->
            app.profile.config.attendance.groupConsecutiveDays = isChecked
        }
        b.showPresenceInMonth.onChange { _, isChecked ->
            app.profile.config.attendance.showPresenceInMonth = isChecked
        }
    }
}

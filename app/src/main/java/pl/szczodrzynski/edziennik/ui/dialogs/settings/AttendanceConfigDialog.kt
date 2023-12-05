/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.AttendanceConfigDialogBinding
import pl.szczodrzynski.edziennik.ext.onChange
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ext.setOnSelectedListener
import pl.szczodrzynski.edziennik.ui.dialogs.base.ConfigDialog
import pl.szczodrzynski.edziennik.utils.managers.AttendanceManager.Companion.SORTED_BY_ALPHABET
import pl.szczodrzynski.edziennik.utils.managers.AttendanceManager.Companion.SORTED_BY_HIGHEST
import pl.szczodrzynski.edziennik.utils.managers.AttendanceManager.Companion.SORTED_BY_LOWEST

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

    override suspend fun loadConfig() {
        b.useSymbols.isChecked = app.profile.config.attendance.useSymbols
        b.groupConsecutiveDays.isChecked = app.profile.config.attendance.groupConsecutiveDays
        b.showPresenceInMonth.isChecked = app.profile.config.attendance.showPresenceInMonth
        b.showDifference.isChecked = app.profile.config.attendance.showDifference

        when (app.profile.config.attendance.orderBy) {
            SORTED_BY_ALPHABET -> b.sortAttendanceByAlphabet
            SORTED_BY_LOWEST -> b.sortAttendanceByLowest
            SORTED_BY_HIGHEST -> b.sortAttendanceByHighest
            else -> null
        }?.isChecked = true
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
        b.showDifference.onChange { _, isChecked ->
            app.profile.config.attendance.showDifference = isChecked
        }

        b.sortAttendanceByAlphabet.setOnSelectedListener { app.profile.config.attendance.orderBy = SORTED_BY_ALPHABET }
        b.sortAttendanceByHighest.setOnSelectedListener { app.profile.config.attendance.orderBy = SORTED_BY_HIGHEST }
        b.sortAttendanceByLowest.setOnSelectedListener { app.profile.config.attendance.orderBy = SORTED_BY_LOWEST }

        b.showDifferenceHelp.onClick {
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.attendance_config_show_difference)
                .setMessage(R.string.attendance_config_show_difference_message)
                .setPositiveButton(R.string.ok, null)
                .show()
        }
    }
}

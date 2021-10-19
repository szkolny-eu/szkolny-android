/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import android.annotation.SuppressLint
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.AttendanceConfigDialogBinding
import pl.szczodrzynski.edziennik.ext.onChange

class AttendanceConfigDialog(
        val activity: AppCompatActivity,
        private val reloadOnDismiss: Boolean = true,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) {
    companion object {
        const val TAG = "GradesConfigDialog"
    }

    private val app by lazy { activity.application as App }
    private val profileConfig by lazy { app.config.getFor(app.profileId).attendance }

    private lateinit var b: AttendanceConfigDialogBinding
    private lateinit var dialog: AlertDialog

    init { run {
        if (activity.isFinishing)
            return@run
        b = AttendanceConfigDialogBinding.inflate(activity.layoutInflater)
        onShowListener?.invoke(TAG)
        dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.menu_attendance_config)
                .setView(b.root)
                .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                .setOnDismissListener {
                    saveConfig()
                    onDismissListener?.invoke(TAG)
                    if (reloadOnDismiss) (activity as? MainActivity)?.reloadTarget()
                }
                .create()
        initView()
        loadConfig()
        dialog.show()
    }}

    @SuppressLint("SetTextI18n")
    private fun loadConfig() {
        b.useSymbols.isChecked = profileConfig.useSymbols
        b.groupConsecutiveDays.isChecked = profileConfig.groupConsecutiveDays
        b.showPresenceInMonth.isChecked = profileConfig.showPresenceInMonth
    }

    private fun saveConfig() {
        // nothing to do here, yet
    }

    private fun initView() {
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

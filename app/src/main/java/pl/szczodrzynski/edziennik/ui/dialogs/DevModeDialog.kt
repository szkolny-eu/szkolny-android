/*
 * Copyright (c) Kuba Szczodrzyński 2024-7-5.
 */

package pl.szczodrzynski.edziennik.ui.dialogs

import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.base.dialog.BaseDialog

class DevModeDialog(
    activity: AppCompatActivity,
) : BaseDialog<Unit>(activity) {

    override fun getTitleRes() = R.string.are_you_sure
    override fun getMessageRes() = R.string.dev_mode_enable_warning
    override fun isCancelable() = false
    override fun getPositiveButtonText() = R.string.yes
    override fun getNegativeButtonText() = R.string.no

    override suspend fun onPositiveClick(): Boolean {
        app.config.devMode = true
        if (!App.devMode) {
            RestartDialog(activity).showModal()
            return NO_DISMISS
        }
        return DISMISS
    }

    override suspend fun onNegativeClick(): Boolean {
        app.config.devMode = null
        if (App.devMode) {
            RestartDialog(activity).showModal()
            return NO_DISMISS
        }
        return DISMISS
    }
}

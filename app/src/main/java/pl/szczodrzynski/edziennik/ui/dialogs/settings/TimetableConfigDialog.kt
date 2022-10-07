/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-7.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.TimetableConfigDialogBinding
import pl.szczodrzynski.edziennik.ui.dialogs.base.ConfigDialog

class TimetableConfigDialog(
    activity: AppCompatActivity,
    reloadOnDismiss: Boolean = true,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : ConfigDialog<TimetableConfigDialogBinding>(
    activity,
    reloadOnDismiss,
    onShowListener,
    onDismissListener,
) {

    override val TAG = "TimetableConfigDialog"

    override fun getTitleRes() = R.string.menu_timetable_config
    override fun inflate(layoutInflater: LayoutInflater) =
        TimetableConfigDialogBinding.inflate(layoutInflater)

    private val profileConfig by lazy { app.config.getFor(app.profileId).ui }

    override suspend fun loadConfig() {
        b.config = profileConfig
    }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-7.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.TimetableConfigDialogBinding
import pl.szczodrzynski.edziennik.ext.Intent
import pl.szczodrzynski.edziennik.ui.dialogs.base.ConfigDialog
import pl.szczodrzynski.edziennik.ui.timetable.TimetableFragment

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

    override fun initView() {
        b.features = app.profile.loginStoreType.features
    }

    override suspend fun loadConfig() {
        b.config = profileConfig
    }

    override suspend fun saveConfig() {
        activity.sendBroadcast(Intent(TimetableFragment.ACTION_RELOAD_PAGES))
    }
}

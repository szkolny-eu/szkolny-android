/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-18.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ext.resolveString
import pl.szczodrzynski.edziennik.ui.base.enums.NavTarget
import pl.szczodrzynski.edziennik.ui.base.enums.NavTargetLocation
import pl.szczodrzynski.edziennik.ui.dialogs.base.BaseDialog

class MiniMenuConfigDialog(
    activity: AppCompatActivity,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BaseDialog<NavTarget>(activity, onShowListener, onDismissListener) {

    override val TAG = "BellSyncTimeChooseDialog"

    override fun getTitleRes() = R.string.settings_theme_mini_drawer_buttons_dialog_title
    override fun getMessageRes() = R.string.settings_theme_mini_drawer_buttons_dialog_text
    override fun getPositiveButtonText() = R.string.ok
    override fun getNegativeButtonText() = R.string.cancel

    @Suppress("USELESS_CAST")
    override fun getMultiChoiceItems() = NavTarget.values()
        .filter {
            (!it.devModeOnly || App.devMode) && it.location in listOf(
                NavTargetLocation.DRAWER,
                // NavTargetLocation.DRAWER_MORE,
                NavTargetLocation.DRAWER_BOTTOM,
            )
        }
        .associateBy { it.nameRes.resolveString(activity) as CharSequence }

    override fun getDefaultSelectedItems() = app.config.ui.miniMenuButtons.toSet()

    override suspend fun onShow() = Unit

    override suspend fun onPositiveClick(): Boolean {
        app.config.ui.miniMenuButtons = getMultiSelection().toList()
        if (activity is MainActivity) {
            activity.setDrawerItems()
            activity.drawer.updateBadges()
        }
        return DISMISS
    }
}

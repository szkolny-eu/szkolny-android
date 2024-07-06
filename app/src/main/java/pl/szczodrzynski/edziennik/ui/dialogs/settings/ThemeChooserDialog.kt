/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-18.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.enums.Theme
import pl.szczodrzynski.edziennik.ui.base.dialog.BaseDialog

class ThemeChooserDialog(
    activity: AppCompatActivity,
) : BaseDialog<Theme>(activity) {

    override fun getTitleRes() = R.string.settings_theme_theme_text
    override fun getPositiveButtonText() = R.string.ok
    override fun getNegativeButtonText() = R.string.cancel

    override fun getSingleChoiceItems(): Map<CharSequence, Theme> =
        Theme.entries.associateBy { activity.getString(it.nameRes) }

    override fun getDefaultSelectedItem() = app.uiManager.themeColor

    override suspend fun onPositiveClick(): Boolean {
        val themeColor = getSingleSelection() ?: return DISMISS
        if (app.uiManager.themeColor != themeColor) {
            app.config.ui.themeColor = themeColor
            activity.recreate()
        }
        return DISMISS
    }
}

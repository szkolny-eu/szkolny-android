/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-18.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.enums.Theme
import pl.szczodrzynski.edziennik.ui.dialogs.base.BaseDialog

class ThemeChooserDialog(
    activity: AppCompatActivity,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BaseDialog<Any>(activity, onShowListener, onDismissListener) {

    override val TAG = "ThemeChooserDialog"

    override fun getTitleRes() = R.string.settings_theme_theme_text
    override fun getPositiveButtonText() = R.string.ok
    override fun getNegativeButtonText() = R.string.cancel

    override fun getSingleChoiceItems(): Map<CharSequence, Any> = Theme.entries.associate {
        activity.getString(it.nameRes) to it.ordinal
    }

    override fun getDefaultSelectedItem() = app.uiManager.themeColor.id

    override suspend fun onShow() = Unit

    override suspend fun onPositiveClick(): Boolean {
        val themeId = getSingleSelection() as? Int ?: return DISMISS
        if (app.uiManager.themeColor.ordinal != themeId) {
            app.config.ui.themeConfig = Theme.Config(
                color = enumValues<Theme>()[themeId],
            )
            activity.recreate()
        }
        return DISMISS
    }
}

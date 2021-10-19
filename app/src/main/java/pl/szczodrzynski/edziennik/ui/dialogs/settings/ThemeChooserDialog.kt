/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-18.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.dialogs.base.BaseDialog
import pl.szczodrzynski.edziennik.utils.Themes

class ThemeChooserDialog(
    activity: AppCompatActivity,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BaseDialog(activity, onShowListener, onDismissListener) {

    override val TAG = "ThemeChooserDialog"

    override fun getTitleRes() = R.string.settings_theme_theme_text
    override fun getPositiveButtonText() = R.string.ok
    override fun getNegativeButtonText() = R.string.cancel

    override fun getSingleChoiceItems(): Map<CharSequence, Any> = Themes.themeList.associate {
        activity.getString(it.name) to it.id
    }

    override fun getDefaultSelectedItem() = Themes.theme.id

    override suspend fun onShow() = Unit

    override suspend fun onPositiveClick(): Boolean {
        val themeId = getSingleSelection() as? Int ?: return DISMISS
        if (app.config.ui.theme != themeId) {
            app.config.ui.theme = themeId
            Themes.themeInt = themeId
            activity.recreate()
        }
        return DISMISS
    }
}

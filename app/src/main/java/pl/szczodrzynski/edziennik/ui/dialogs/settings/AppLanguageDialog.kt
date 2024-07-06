/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-19.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.base.dialog.BaseDialog

class AppLanguageDialog(
    activity: AppCompatActivity,
) : BaseDialog<Any>(activity) {

    override fun getTitleRes() = R.string.app_language_dialog_title
    override fun getMessage() = activity.getString(R.string.app_language_dialog_text)
    override fun getPositiveButtonText() = R.string.ok
    override fun getNegativeButtonText() = R.string.cancel

    override fun getSingleChoiceItems(): Map<CharSequence, Any> = mapOf(
        R.string.language_system to "",
        R.string.language_polish to "pl",
        R.string.language_english to "en",
        R.string.language_german to "de",
    ).mapKeys { (resId, _) -> activity.getString(resId) }

    override fun getDefaultSelectedItem() = app.config.ui.language

    override suspend fun onPositiveClick(): Boolean {
        val language = getSingleSelection() as? String ?: return DISMISS
        if (language.isEmpty())
            app.config.ui.language = null
        else
            app.config.ui.language = language
        activity.recreate()
        return NO_DISMISS
    }
}

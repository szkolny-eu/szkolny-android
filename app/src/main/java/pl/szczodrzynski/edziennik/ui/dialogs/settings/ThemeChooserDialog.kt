/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-18.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.utils.Themes
import kotlin.coroutines.CoroutineContext

class ThemeChooserDialog(
    val activity: AppCompatActivity,
    val onShowListener: ((tag: String) -> Unit)? = null,
    val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {
    companion object {
        private const val TAG = "ThemeChooserDialog"
    }

    private lateinit var app: App
    private lateinit var dialog: AlertDialog

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local variables go here

    init { run {
        if (activity.isFinishing)
            return@run
        onShowListener?.invoke(TAG)
        app = activity.applicationContext as App

        dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.settings_theme_theme_text)
            .setSingleChoiceItems(
                Themes.getThemeNames(activity).toTypedArray(),
                Themes.themeIndex,
                null
            )
            .setPositiveButton(R.string.ok) { _, _ ->
                val which = dialog.listView.checkedItemPosition

                val theme = Themes.themeList[which]
                if (app.config.ui.theme == theme.id)
                    return@setPositiveButton
                app.config.ui.theme = theme.id
                Themes.themeIndex = which
                activity.recreate()
            }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener {
                onDismissListener?.invoke(TAG)
            }
            .show()
    }}
}

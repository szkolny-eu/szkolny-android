/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-19.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.*
import kotlin.coroutines.CoroutineContext

class AppLanguageDialog(
    val activity: AppCompatActivity,
    val onShowListener: ((tag: String) -> Unit)? = null,
    val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {
    companion object {
        private const val TAG = "AppLanguageDialog"
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

        val languages = mapOf(
            null to R.string.language_system,
            "pl" to R.string.language_polish,
            "en" to R.string.language_english,
            "de" to R.string.language_german
        )
        val languageIds = languages.map { it.key }
        val languageNames = languages.map {
            activity.getString(it.value)
        }

        dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.app_language_dialog_title)
            //.setMessage(R.string.settings_about_language_dialog_text)
            .setSingleChoiceItems(
                languageNames.toTypedArray(),
                languageIds.indexOf(app.config.ui.language),
                null
            )
            .setPositiveButton(R.string.ok) { _, _ ->
                val which = dialog.listView.checkedItemPosition

                app.config.ui.language = languageIds[which]
                activity.recreate()
            }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener {
                onDismissListener?.invoke(TAG)
            }
            .show()
    }}
}

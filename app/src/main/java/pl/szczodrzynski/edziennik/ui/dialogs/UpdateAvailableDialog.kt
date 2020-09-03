/*
 * Copyright (c) Kuba Szczodrzyński 2020-9-3.
 */

package pl.szczodrzynski.edziennik.ui.dialogs

import android.text.Html
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.szkolny.response.Update
import pl.szczodrzynski.edziennik.sync.UpdateDownloaderService
import kotlin.coroutines.CoroutineContext

class UpdateAvailableDialog(
        val activity: AppCompatActivity,
        val update: Update,
        val mandatory: Boolean = update.updateMandatory,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {
    companion object {
        private const val TAG = "UpdateAvailableDialog"
    }

    private lateinit var app: App
    private lateinit var dialog: AlertDialog

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    init { run {
        if (activity.isFinishing)
            return@run
        if (update.versionCode <= BuildConfig.VERSION_CODE)
            return@run
        onShowListener?.invoke(TAG)
        app = activity.applicationContext as App

        dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.update_available_title)
                .setMessage(
                        R.string.update_available_format,
                        BuildConfig.VERSION_NAME,
                        update.versionName,
                        update.releaseNotes?.let { Html.fromHtml(it) } ?: "---"
                )
                .setPositiveButton(R.string.update_available_button) { dialog, _ ->
                    activity.startService(Intent(app, UpdateDownloaderService::class.java))
                    dialog.dismiss()
                }
                .also {
                    if (!mandatory)
                        it.setNeutralButton(R.string.update_available_later, null)
                }
                .setCancelable(!mandatory)
                .setOnDismissListener {
                    onDismissListener?.invoke(TAG)
                }
                .show()
    }}
}

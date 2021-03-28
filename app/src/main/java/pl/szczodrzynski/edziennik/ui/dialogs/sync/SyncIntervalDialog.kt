/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-20.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.sync

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.*
import kotlin.coroutines.CoroutineContext

class SyncIntervalDialog(
    val activity: AppCompatActivity,
    val onChangeListener: (() -> Unit)? = null,
    val onShowListener: ((tag: String) -> Unit)? = null,
    val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {
    companion object {
        private const val TAG = "SyncIntervalDialog"
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

        val intervals = listOf(
            30 * MINUTE,
            45 * MINUTE,
            60 * MINUTE,
            90 * MINUTE,
            2 * HOUR,
            3 * HOUR,
            4 * HOUR,
            6 * HOUR,
            10 * HOUR
        )
        val intervalNames = intervals.map {
            activity.getSyncInterval(it.toInt())
        }

        dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.settings_sync_sync_interval_dialog_title)
            //.setMessage(R.string.settings_sync_sync_interval_dialog_text)
            .setSingleChoiceItems(
                intervalNames.toTypedArray(),
                intervals.indexOf(app.config.sync.interval.toLong()),
                null
            )
            .setPositiveButton(R.string.ok) { _, _ ->
                val which = dialog.listView.checkedItemPosition

                val interval = intervals[which]
                app.config.sync.interval = interval.toInt()
                onChangeListener?.invoke()
            }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener {
                onDismissListener?.invoke(TAG)
            }
            .show()
    }}
}

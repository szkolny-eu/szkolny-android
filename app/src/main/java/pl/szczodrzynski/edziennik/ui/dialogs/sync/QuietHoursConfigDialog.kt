/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-20.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.sync

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.utils.models.Time
import kotlin.coroutines.CoroutineContext

class QuietHoursConfigDialog(
    val activity: AppCompatActivity,
    val onChangeListener: (() -> Unit)? = null,
    val onShowListener: ((tag: String) -> Unit)? = null,
    val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {
    companion object {
        private const val TAG = "QuietHoursConfigDialog"
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
            .setTitle(R.string.settings_sync_quiet_hours_dialog_title)
            .setItems(arrayOf(
                activity.getString(R.string.settings_sync_quiet_hours_set_beginning),
                activity.getString(R.string.settings_sync_quiet_hours_set_end)
            )) { dialog, which ->
                when (which) {
                    0 -> configStartTime()
                    1 -> configEndTime()
                }
                dialog.dismiss()
            }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener {
                onDismissListener?.invoke(TAG)
            }
            .show()
    }}

    private fun configStartTime() {
        onShowListener?.invoke(TAG + "Start")

        val time = app.config.sync.quietHoursStart ?: return
        val picker = MaterialTimePicker.Builder()
            .setTitleText(R.string.settings_sync_quiet_hours_set_beginning)
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(time.hour)
            .setMinute(time.minute)
            .build()

        picker.show(activity.supportFragmentManager, TAG)
        picker.addOnPositiveButtonClickListener {
            app.config.sync.quietHoursEnabled = true
            app.config.sync.quietHoursStart = Time(picker.hour, picker.minute, 0)
            onChangeListener?.invoke()
        }
        picker.addOnDismissListener {
            onDismissListener?.invoke(TAG + "Start")
        }
    }

    private fun configEndTime() {
        onShowListener?.invoke(TAG + "End")

        val time = app.config.sync.quietHoursEnd ?: return
        val picker = MaterialTimePicker.Builder()
            .setTitleText(R.string.settings_sync_quiet_hours_set_end)
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(time.hour)
            .setMinute(time.minute)
            .build()

        picker.show(activity.supportFragmentManager, TAG)
        picker.addOnPositiveButtonClickListener {
            app.config.sync.quietHoursEnabled = true
            app.config.sync.quietHoursEnd = Time(picker.hour, picker.minute, 0)
            onChangeListener?.invoke()
        }
        picker.addOnDismissListener {
            onDismissListener?.invoke(TAG + "End")
        }
    }
}

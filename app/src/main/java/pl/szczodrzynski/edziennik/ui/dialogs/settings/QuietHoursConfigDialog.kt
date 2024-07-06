/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-20.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ext.resolveString
import pl.szczodrzynski.edziennik.ui.base.dialog.BaseDialog
import pl.szczodrzynski.edziennik.utils.models.Time

class QuietHoursConfigDialog(
    activity: AppCompatActivity,
    val onChangeListener: (() -> Unit)? = null,
) : BaseDialog<Int>(activity) {
    companion object {
        private const val TAG = "QuietHoursConfigDialog"
    }

    override fun getTitleRes() = R.string.settings_sync_quiet_hours_dialog_title
    override fun getNegativeButtonText() = R.string.cancel

    override fun getItems(): Map<CharSequence, Int> = mapOf(
        R.string.settings_sync_quiet_hours_set_beginning.resolveString(activity) to 0,
        R.string.settings_sync_quiet_hours_set_end.resolveString(activity) to 1,
    )

    override suspend fun onItemClick(item: Int): Boolean {
        when (item) {
            0 -> configStartTime()
            1 -> configEndTime()
        }
        return NO_DISMISS
    }

    private fun configStartTime() {
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
    }

    private fun configEndTime() {
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
    }
}

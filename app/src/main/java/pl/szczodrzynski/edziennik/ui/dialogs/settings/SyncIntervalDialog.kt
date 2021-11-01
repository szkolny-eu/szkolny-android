/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-20.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ext.HOUR
import pl.szczodrzynski.edziennik.ext.MINUTE
import pl.szczodrzynski.edziennik.ext.getSyncInterval
import pl.szczodrzynski.edziennik.ui.dialogs.base.BaseDialog

class SyncIntervalDialog(
    activity: AppCompatActivity,
    private val onChangeListener: (() -> Unit)? = null,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BaseDialog(activity, onShowListener, onDismissListener) {

    override val TAG = "SyncIntervalDialog"

    override fun getTitleRes() = R.string.settings_sync_sync_interval_dialog_title
    override fun getMessageRes() = R.string.settings_sync_sync_interval_dialog_text
    override fun getPositiveButtonText() = R.string.ok
    override fun getNegativeButtonText() = R.string.cancel

    override fun getSingleChoiceItems(): Map<CharSequence, Any> = listOf(
        30 * MINUTE,
        45 * MINUTE,
        60 * MINUTE,
        90 * MINUTE,
        2 * HOUR,
        3 * HOUR,
        4 * HOUR,
        6 * HOUR,
        10 * HOUR,
    ).associateBy { activity.getSyncInterval(it.toInt()) }

    override fun getDefaultSelectedItem() = app.config.sync.interval.toLong()

    override suspend fun onShow() = Unit

    override suspend fun onPositiveClick(): Boolean {
        val interval = getSingleSelection() as? Long ?: return DISMISS
        app.config.sync.interval = interval.toInt()
        onChangeListener?.invoke()
        return DISMISS
    }
}

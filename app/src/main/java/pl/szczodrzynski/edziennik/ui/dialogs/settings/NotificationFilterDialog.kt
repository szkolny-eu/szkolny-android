/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-21.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.enums.NotificationType
import pl.szczodrzynski.edziennik.ext.resolveString
import pl.szczodrzynski.edziennik.ui.dialogs.base.BaseDialog

class NotificationFilterDialog(
    activity: AppCompatActivity,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BaseDialog<NotificationType>(activity, onShowListener, onDismissListener) {

    override val TAG = "NotificationFilterDialog"

    override fun getTitleRes() = R.string.dialog_notification_filter_title
    override fun getMessageRes() = R.string.dialog_notification_filter_text
    override fun getPositiveButtonText() = R.string.ok
    override fun getNegativeButtonText() = R.string.cancel

    @Suppress("USELESS_CAST")
    override fun getMultiChoiceItems() = NotificationType.values()
        .filter { it.enabledByDefault != null }
        .associateBy { it.titleRes.resolveString(activity) as CharSequence }

    override fun getDefaultSelectedItems() = NotificationType.values()
        .filter { it.enabledByDefault != null && it !in app.profile.config.sync.notificationFilter }
        .toSet()

    override suspend fun onShow() = Unit

    override suspend fun onPositiveClick(): Boolean {
        val enabledTypes = getMultiSelection()
        val disabledTypes = NotificationType.values()
            .filter { it.enabledByDefault != null && it !in enabledTypes }
            .toSet()

        if (disabledTypes.any { it.enabledByDefault == true }) {
            // warn user when he tries to disable some notifications
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.are_you_sure)
                .setMessage(R.string.notification_filter_warning)
                .setPositiveButton(R.string.ok) { _, _ ->
                    app.profile.config.sync.notificationFilter = disabledTypes
                    dismiss()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
            return NO_DISMISS
        }

        app.profile.config.sync.notificationFilter = disabledTypes

        return DISMISS
    }
}

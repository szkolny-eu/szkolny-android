/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-21.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Notification
import pl.szczodrzynski.edziennik.ui.dialogs.base.BaseDialog

class NotificationFilterDialog(
    activity: AppCompatActivity,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BaseDialog(activity, onShowListener, onDismissListener) {

    override val TAG = "NotificationFilterDialog"

    override fun getTitleRes() = R.string.dialog_notification_filter_title
    override fun getMessageRes() = R.string.dialog_notification_filter_text
    override fun getPositiveButtonText() = R.string.ok
    override fun getNegativeButtonText() = R.string.cancel

    override fun getMultiChoiceItems(): Map<CharSequence, Any> {
        notificationTypes = mapOf(
            R.string.notification_type_timetable_lesson_change to Notification.TYPE_TIMETABLE_LESSON_CHANGE,
            R.string.notification_type_new_grade to Notification.TYPE_NEW_GRADE,
            R.string.notification_type_new_event to Notification.TYPE_NEW_EVENT,
            R.string.notification_type_new_homework to Notification.TYPE_NEW_HOMEWORK,
            R.string.notification_type_new_message to Notification.TYPE_NEW_MESSAGE,
            R.string.notification_type_lucky_number to Notification.TYPE_LUCKY_NUMBER,
            R.string.notification_type_notice to Notification.TYPE_NEW_NOTICE,
            R.string.notification_type_attendance to Notification.TYPE_NEW_ATTENDANCE,
            R.string.notification_type_new_announcement to Notification.TYPE_NEW_ANNOUNCEMENT,
            R.string.notification_type_new_shared_event to Notification.TYPE_NEW_SHARED_EVENT,
            R.string.notification_type_new_shared_homework to Notification.TYPE_NEW_SHARED_HOMEWORK,
            R.string.notification_type_removed_shared_event to Notification.TYPE_REMOVED_SHARED_EVENT,
            R.string.notification_type_new_teacher_absence to Notification.TYPE_TEACHER_ABSENCE,
        ).mapKeys { (resId, _) -> activity.getString(resId) }
        return notificationTypes
    }

    override fun getDefaultSelectedItems() =
        notificationTypes.values.subtract(app.config.forProfile().sync.notificationFilter)

    override suspend fun onShow() = Unit

    private lateinit var notificationTypes: Map<CharSequence, Int>

    override suspend fun onPositiveClick(): Boolean {
        val enabledTypes = getMultiSelection().filterIsInstance<Int>()
        val disabledTypes = notificationTypes.values.subtract(enabledTypes).toList()

        if (disabledTypes.isNotEmpty()) {
            // warn user when he tries to disable some notifications
            MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.are_you_sure)
                .setMessage(R.string.notification_filter_warning)
                .setPositiveButton(R.string.ok) { _, _ ->
                    app.config.forProfile().sync.notificationFilter = disabledTypes
                    dismiss()
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
            return NO_DISMISS
        }

        app.config.forProfile().sync.notificationFilter = disabledTypes

        return DISMISS
    }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-21.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.sync

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Notification
import pl.szczodrzynski.edziennik.onClick
import kotlin.coroutines.CoroutineContext

// TODO refactor dialog to allow configuring other profiles
// than the selected one in UI
class NotificationFilterDialog(
        val activity: AppCompatActivity,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {
    companion object {
        private const val TAG = "NotificationFilterDialog"
        private val notificationTypes = listOf(
                Notification.TYPE_TIMETABLE_LESSON_CHANGE to R.string.notification_type_timetable_lesson_change,
                Notification.TYPE_NEW_GRADE to R.string.notification_type_new_grade,
                Notification.TYPE_NEW_EVENT to R.string.notification_type_new_event,
                Notification.TYPE_NEW_HOMEWORK to R.string.notification_type_new_homework,
                Notification.TYPE_NEW_MESSAGE to R.string.notification_type_new_message,
                Notification.TYPE_LUCKY_NUMBER to R.string.notification_type_lucky_number,
                Notification.TYPE_NEW_NOTICE to R.string.notification_type_notice,
                Notification.TYPE_NEW_ATTENDANCE to R.string.notification_type_attendance,
                Notification.TYPE_NEW_ANNOUNCEMENT to R.string.notification_type_new_announcement,
                Notification.TYPE_NEW_SHARED_EVENT to R.string.notification_type_new_shared_event,
                Notification.TYPE_NEW_SHARED_HOMEWORK to R.string.notification_type_new_shared_homework,
                Notification.TYPE_REMOVED_SHARED_EVENT to R.string.notification_type_removed_shared_event
        )
    }

    private lateinit var app: App
    private lateinit var dialog: AlertDialog

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private val notificationFilter = mutableListOf<Int>()

    init { run {
        if (activity.isFinishing)
            return@run
        onShowListener?.invoke(TAG)
        app = activity.applicationContext as App

        notificationFilter.clear()
        notificationFilter += app.config.forProfile().sync.notificationFilter
        val items = notificationTypes.map { app.getString(it.second) }.toTypedArray()
        val checkedItems = notificationTypes.map { !notificationFilter.contains(it.first) }.toBooleanArray()

        dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.dialog_notification_filter_title)
                //.setMessage(R.string.dialog_notification_filter_text)
                .setMultiChoiceItems(items, checkedItems) { _, which, isChecked ->
                    val type = notificationTypes[which].first
                    notificationFilter.remove(type)
                    if (!isChecked)
                        notificationFilter += type
                }
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .setOnDismissListener {
                    onDismissListener?.invoke(TAG)
                }
                .show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.onClick {
            if (notificationFilter.isEmpty()) {
                app.config.forProfile().sync.notificationFilter = notificationFilter
                dialog.dismiss()
                return@onClick
            }
            // warn user when he tries to disable some notifications
            MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.are_you_sure)
                    .setMessage(R.string.notification_filter_warning)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        app.config.forProfile().sync.notificationFilter = notificationFilter
                        dialog.dismiss()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
        }
    }}
}

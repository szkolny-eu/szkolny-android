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
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_AGENDA
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_ANNOUNCEMENTS
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_ATTENDANCE
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_BEHAVIOUR
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_GRADES
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_HOME
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_HOMEWORK
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_MESSAGES
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_NOTIFICATIONS
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_SETTINGS
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_TIMETABLE
import pl.szczodrzynski.edziennik.R
import kotlin.coroutines.CoroutineContext

class MiniMenuConfigDialog(
    val activity: AppCompatActivity,
    val onShowListener: ((tag: String) -> Unit)? = null,
    val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {
    companion object {
        private const val TAG = "MiniMenuConfigDialog"
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

        val buttons = mapOf(
            DRAWER_ITEM_HOME to R.string.menu_home_page,
            DRAWER_ITEM_TIMETABLE to R.string.menu_timetable,
            DRAWER_ITEM_AGENDA to R.string.menu_agenda,
            DRAWER_ITEM_GRADES to R.string.menu_grades,
            DRAWER_ITEM_MESSAGES to R.string.menu_messages,
            DRAWER_ITEM_HOMEWORK to R.string.menu_homework,
            DRAWER_ITEM_BEHAVIOUR to R.string.menu_notices,
            DRAWER_ITEM_ATTENDANCE to R.string.menu_attendance,
            DRAWER_ITEM_ANNOUNCEMENTS to R.string.menu_announcements,
            DRAWER_ITEM_NOTIFICATIONS to R.string.menu_notifications,
            DRAWER_ITEM_SETTINGS to R.string.menu_settings
        )
        val miniMenuButtons = app.config.ui.miniMenuButtons

        dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.settings_theme_mini_drawer_buttons_dialog_title)
            //.setMessage(R.string.settings_theme_mini_drawer_buttons_dialog_text)
            .setMultiChoiceItems(
                buttons.map { activity.getString(it.value) }.toTypedArray(),
                buttons.map { it.key in miniMenuButtons }.toBooleanArray(),
                null
            )
            .setPositiveButton(R.string.ok) { _, _ ->
                app.config.ui.miniMenuButtons =
                    buttons.keys.mapIndexedNotNull { index, id ->
                        if (dialog.listView.checkedItemPositions[index])
                            id
                        else
                            null
                    }

                if (activity is MainActivity) {
                    activity.setDrawerItems()
                    activity.drawer.updateBadges()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .setOnDismissListener {
                onDismissListener?.invoke(TAG)
            }
            .show()
    }}
}

/*
 * Copyright (c) Kuba Szczodrzyński 2021-3-18.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.settings

import androidx.appcompat.app.AppCompatActivity
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
import pl.szczodrzynski.edziennik.ui.dialogs.base.BaseDialog

class MiniMenuConfigDialog(
    activity: AppCompatActivity,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BaseDialog(activity, onShowListener, onDismissListener) {

    override val TAG = "BellSyncTimeChooseDialog"

    override fun getTitleRes() = R.string.settings_theme_mini_drawer_buttons_dialog_title
    override fun getMessageRes() = R.string.settings_theme_mini_drawer_buttons_dialog_text
    override fun getPositiveButtonText() = R.string.ok
    override fun getNegativeButtonText() = R.string.cancel

    override fun getMultiChoiceItems(): Map<CharSequence, Any> = mapOf(
        R.string.menu_home_page to DRAWER_ITEM_HOME,
        R.string.menu_timetable to DRAWER_ITEM_TIMETABLE,
        R.string.menu_agenda to DRAWER_ITEM_AGENDA,
        R.string.menu_grades to DRAWER_ITEM_GRADES,
        R.string.menu_messages to DRAWER_ITEM_MESSAGES,
        R.string.menu_homework to DRAWER_ITEM_HOMEWORK,
        R.string.menu_notices to DRAWER_ITEM_BEHAVIOUR,
        R.string.menu_attendance to DRAWER_ITEM_ATTENDANCE,
        R.string.menu_announcements to DRAWER_ITEM_ANNOUNCEMENTS,
        R.string.menu_notifications to DRAWER_ITEM_NOTIFICATIONS,
        R.string.menu_settings to DRAWER_ITEM_SETTINGS,
    ).mapKeys { (resId, _) -> activity.getString(resId) }

    override fun getDefaultSelectedItems() = app.config.ui.miniMenuButtons.toSet()

    override suspend fun onShow() = Unit

    override suspend fun onPositiveClick(): Boolean {
        app.config.ui.miniMenuButtons = getMultiSelection().filterIsInstance<Int>()
        if (activity is MainActivity) {
            activity.setDrawerItems()
            activity.drawer.updateBadges()
        }
        return DISMISS
    }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-13.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.sync

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
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_TIMETABLE
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.ui.dialogs.base.BaseDialog
import pl.szczodrzynski.edziennik.ui.messages.list.MessagesFragment

class SyncViewListDialog(
    activity: MainActivity,
    private val currentViewId: Int,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BaseDialog(activity, onShowListener, onDismissListener) {

    override val TAG = "SyncViewListDialog"

    override fun getTitleRes() = R.string.dialog_sync_view_list_title
    override fun getPositiveButtonText() = R.string.ok
    override fun getNeutralButtonText() = R.string.sync_feature_all
    override fun getNegativeButtonText() = R.string.cancel

    override fun getMultiChoiceItems(): Map<CharSequence, Any> {
        items = mapOf(
            R.string.menu_timetable to DRAWER_ITEM_TIMETABLE,
            R.string.menu_agenda to DRAWER_ITEM_AGENDA,
            R.string.menu_grades to DRAWER_ITEM_GRADES,
            R.string.menu_homework to DRAWER_ITEM_HOMEWORK,
            R.string.menu_notices to DRAWER_ITEM_BEHAVIOUR,
            R.string.menu_attendance to DRAWER_ITEM_ATTENDANCE,
            R.string.title_messages_inbox_single to (DRAWER_ITEM_MESSAGES to 0),
            R.string.title_messages_sent_single to (DRAWER_ITEM_MESSAGES to 1),
            R.string.menu_announcements to DRAWER_ITEM_ANNOUNCEMENTS,
        ).mapKeys { (resId, _) -> activity.getString(resId) }
        return items
    }

    override fun getDefaultSelectedItems(): Set<Any> {
        val everything = currentViewId == DRAWER_ITEM_HOME
        return when {
            everything -> items.values.toSet()
            currentViewId == DRAWER_ITEM_MESSAGES -> when (MessagesFragment.pageSelection) {
                1 -> setOf(DRAWER_ITEM_MESSAGES to 1)
                else -> setOf(DRAWER_ITEM_MESSAGES to 0)
            }
            else -> setOf(currentViewId)
        }
    }

    override suspend fun onShow() = Unit

    private lateinit var items: Map<CharSequence, Any>

    @Suppress("UNCHECKED_CAST")
    override suspend fun onPositiveClick(): Boolean {
        val selected = getMultiSelection().mapNotNull {
            when (it) {
                is Int -> it to 0
                is Pair<*, *> -> it as Pair<Int, Int>
                else -> null
            }
        }

        if (selected.isEmpty())
            return DISMISS

        if (activity is MainActivity)
            activity.swipeRefreshLayout.isRefreshing = true
        EdziennikTask.syncProfile(
            App.profileId,
            selected
        ).enqueue(activity)
        return DISMISS
    }

    override suspend fun onNeutralClick(): Boolean {
        if (activity is MainActivity)
            activity.swipeRefreshLayout.isRefreshing = true
        EdziennikTask.syncProfile(App.profileId).enqueue(activity)
        return DISMISS
    }
}

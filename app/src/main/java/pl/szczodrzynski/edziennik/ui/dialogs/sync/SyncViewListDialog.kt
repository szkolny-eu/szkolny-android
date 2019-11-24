/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-13.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.sync

import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.api.v2.events.task.EdziennikTask
import pl.szczodrzynski.edziennik.databinding.DialogLessonDetailsBinding
import pl.szczodrzynski.edziennik.ui.modules.messages.MessagesFragment
import kotlin.coroutines.CoroutineContext

class SyncViewListDialog(
        val activity: MainActivity,
        val currentViewId: Int? = null
) : CoroutineScope {
    companion object {
        private const val TAG = "SyncViewListDialog"
    }

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private val app by lazy { activity.application as App }
    private lateinit var b: DialogLessonDetailsBinding
    private lateinit var dialog: AlertDialog

    init { run {
        job = Job()

        val viewIds = arrayOf(
                MainActivity.DRAWER_ITEM_TIMETABLE,
                MainActivity.DRAWER_ITEM_AGENDA,
                MainActivity.DRAWER_ITEM_GRADES,
                MainActivity.DRAWER_ITEM_HOMEWORK,
                MainActivity.DRAWER_ITEM_BEHAVIOUR,
                MainActivity.DRAWER_ITEM_ATTENDANCE,
                MainActivity.DRAWER_ITEM_MESSAGES,
                MainActivity.DRAWER_ITEM_MESSAGES,
                MainActivity.DRAWER_ITEM_ANNOUNCEMENTS
        )

        val items = arrayOf<String>(
                app.getString(R.string.menu_timetable),
                app.getString(R.string.menu_agenda),
                app.getString(R.string.menu_grades),
                app.getString(R.string.menu_homework),
                app.getString(R.string.menu_notices),
                app.getString(R.string.menu_attendance),
                app.getString(R.string.title_messages_inbox_single),
                app.getString(R.string.title_messages_sent_single),
                app.getString(R.string.menu_announcements)
        )

        val everything = currentViewId == MainActivity.DRAWER_ITEM_HOME
        val checkedItems = booleanArrayOf(
                everything || currentViewId == MainActivity.DRAWER_ITEM_TIMETABLE,
                everything || currentViewId == MainActivity.DRAWER_ITEM_AGENDA,
                everything || currentViewId == MainActivity.DRAWER_ITEM_GRADES,
                everything || currentViewId == MainActivity.DRAWER_ITEM_HOMEWORK,
                everything || currentViewId == MainActivity.DRAWER_ITEM_BEHAVIOUR,
                everything || currentViewId == MainActivity.DRAWER_ITEM_ATTENDANCE,
                everything || currentViewId == MainActivity.DRAWER_ITEM_MESSAGES && MessagesFragment.pageSelection != 1,
                everything || currentViewId == MainActivity.DRAWER_ITEM_MESSAGES && MessagesFragment.pageSelection == 1,
                everything || currentViewId == MainActivity.DRAWER_ITEM_ANNOUNCEMENTS
        )
        val userChooses = checkedItems.toMutableList()

        dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.dialog_sync_view_list_title)
                .setMultiChoiceItems(items, checkedItems) { _, which, isChecked ->
                    userChooses[which] = isChecked
                }
                .setPositiveButton(R.string.ok) { _, _ ->
                    dialog.dismiss()

                    val selectedViewIds = userChooses.mapIndexed { index, it ->
                        if (it)
                            viewIds[index] to when (index) {
                                7 -> 1
                                else -> 0
                            }
                        else
                            null
                    }.let {
                        listOfNotNull(*it.toTypedArray())
                    }

                    if (selectedViewIds.isNotEmpty()) {
                        activity.swipeRefreshLayout.isRefreshing = true
                        EdziennikTask.syncProfile(
                                App.profileId,
                                selectedViewIds
                        ).enqueue(activity)
                    }
                }
                .setNegativeButton(R.string.cancel) { _, _ ->
                    dialog.dismiss()
                }
                .show()
    }}
}

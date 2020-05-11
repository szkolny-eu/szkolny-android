/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-20
 */

package pl.szczodrzynski.edziennik.ui.dialogs.bell

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.databinding.DialogBellSyncTimeChooseBinding
import pl.szczodrzynski.edziennik.utils.TextInputDropDown
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import kotlin.coroutines.CoroutineContext

class BellSyncTimeChooseDialog(
        val activity: AppCompatActivity,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {

    companion object {
        const val TAG = "BellSyncTimeChooseDialog"

        private const val MAX_DIFF_MINUTES = 10
    }

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private lateinit var dialog: AlertDialog
    private lateinit var b: DialogBellSyncTimeChooseBinding

    private val app by lazy { activity.application as App }

    private val today = Date.getToday()
    private val selectedTime: Time?
        get() = b.timeDropdown.selected?.tag as Time?

    init { apply {
        if (activity.isFinishing)
            return@apply
        job = Job()
        b = DialogBellSyncTimeChooseBinding.inflate(activity.layoutInflater)
        onShowListener?.invoke(TAG)
        dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.bell_sync_title)
                .setView(b.root)
                .setPositiveButton(R.string.ok) { dialog, _ ->
                    dialog.dismiss()
                    selectedTime?.let {
                        BellSyncDialog(activity, it)
                    }
                }
                .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                .setOnDismissListener {
                    onDismissListener?.invoke(TAG)
                }
                .create()
                .apply {
                    setButton(AlertDialog.BUTTON_NEUTRAL, app.getString(R.string.reset)) { _, _ ->
                        showResetDialog()
                    }
                }

        initView()
    }}

    private fun initView() {
        b.bellSyncHowto.text = app.getString(R.string.bell_sync_choose_howto)

        app.config.timetable.bellSyncDiff?.let { bellDiff ->
            val multiplier = app.config.timetable.bellSyncMultiplier
            val bellDiffText = (if (multiplier == -1) '-' else '+') + bellDiff.stringHMS
            b.bellSyncHowto.text = app.getString(R.string.concat_2_strings,
                    app.getString(R.string.bell_sync_choose_howto),
                    app.getString(R.string.bell_sync_current_dialog, bellDiffText)
            )
        }

        loadTimeList()
    }

    private fun checkForLessons(timeList: List<Time>): Boolean {
        return if (timeList.isNotEmpty()) {
            val now = Time.getNow()
            val first = timeList.first()
            val last = timeList.last()

            now.stepForward(0, MAX_DIFF_MINUTES, 0) >= first &&
                    now.stepForward(0, -1 * MAX_DIFF_MINUTES, 0) <= last
        } else false
    }

    private fun loadTimeList() { launch {
        val timeItems = withContext(Dispatchers.Default) {
            val lessons = app.db.timetableDao().getAllForDateNow(App.profileId, today)
            val items = mutableListOf<TextInputDropDown.Item>()

            lessons.forEach {
                if (it.type != Lesson.TYPE_NO_LESSONS &&
                        it.type != Lesson.TYPE_CANCELLED &&
                        it.type != Lesson.TYPE_SHIFTED_SOURCE) {

                    items += TextInputDropDown.Item(
                            it.displayStartTime?.value?.toLong() ?: return@forEach,
                            app.getString(R.string.bell_sync_lesson_item, it.displaySubjectName, it.displayStartTime?.stringHM),
                            tag = it.displayStartTime
                    )

                    items += TextInputDropDown.Item(
                            it.displayEndTime?.value?.toLong() ?: return@forEach,
                            app.getString(R.string.bell_sync_break_item, it.displayEndTime?.stringHM),
                            tag = it.displayEndTime
                    )
                }
            }

            items
        }

        if (!checkForLessons(timeItems.map { it.tag as Time })) {
            /* Synchronization not possible */
            MaterialAlertDialogBuilder(activity)
                    .setTitle(R.string.bell_sync_title)
                    .setMessage(R.string.bell_sync_cannot_now)
                    .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                    .show()
        } else {
            b.timeDropdown.clear()
            b.timeDropdown.append(timeItems)
            timeItems.forEachIndexed { index, item ->
                val time = item.tag as Time
                if (time < Time.getNow()) {
                    b.timeDropdown.select(if (timeItems.size > index + 1) timeItems[index + 1] else item)
                }
            }

            b.timeDropdown.isEnabled = true
            // TODO Fix popup cutting off

            dialog.show()
        }
    }}

    private fun showResetDialog() {
        MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.bell_sync_title)
                .setMessage(R.string.bell_sync_reset_confirm)
                .setPositiveButton(R.string.yes) { confirmDialog, _ ->
                    app.config.timetable.bellSyncDiff = null
                    app.config.timetable.bellSyncMultiplier = 0

                    confirmDialog.dismiss()
                    initView()
                    if (activity is MainActivity) activity.reloadTarget()
                }
                .setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }
                .show()
    }
}

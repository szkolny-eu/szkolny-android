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
import pl.szczodrzynski.edziennik.databinding.DialogBellSyncTimeChooseBinding
import pl.szczodrzynski.edziennik.onClick
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
    }

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private lateinit var dialog: AlertDialog
    private lateinit var b: DialogBellSyncTimeChooseBinding

    private val app by lazy { activity.application as App }

    private val today = Date.getToday()
    private val selectedTime: Time?
        get() = b.timeDropdown.selected?.id?.let { Time.fromValue(it.toInt()) }

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
                .setNeutralButton(R.string.reset, null)
                .setOnDismissListener {
                    onDismissListener?.invoke(TAG)
                }
                .show()

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).onClick {
            showResetDialog()
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

    private fun loadTimeList() { launch {
        val timeItems = withContext(Dispatchers.Default) {
            val lessons = app.db.timetableDao().getForDateNow(App.profileId, today)
            val items = mutableListOf<TextInputDropDown.Item>()

            lessons.forEach {
                items += TextInputDropDown.Item(
                        it.startTime?.value?.toLong() ?: return@forEach,
                        app.getString(R.string.bell_sync_lesson_item, it.displaySubjectName, it.startTime?.stringHM),
                        tag = it
                )

                items += TextInputDropDown.Item(
                        it.endTime?.value?.toLong() ?: return@forEach,
                        app.getString(R.string.bell_sync_break_item, it.endTime?.stringHM),
                        tag = it
                )
            }

            items
        }

        b.timeDropdown.clear()
        b.timeDropdown.append(timeItems)
        timeItems.forEachIndexed { index, item ->
            val time = Time.fromValue(item.id.toInt())
            if (time < Time.getNow()) {
                b.timeDropdown.select(if (timeItems.size > index + 1) timeItems[index + 1] else item)
            }
        }

        b.timeDropdown.isEnabled = true
        // TODO Fix popup cutting off
    }}

    private fun showResetDialog() {
        MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.bell_sync_title)
                .setMessage(R.string.bell_sync_reset_confirm)
                .setPositiveButton(R.string.yes) { confirmDialog, _ ->
                    app.config.timetable.bellSyncDiff = null
                    app.config.timetable.bellSyncMultiplier = 0

                    confirmDialog.dismiss()
                    dialog.dismiss()
                    if (activity is MainActivity) activity.reloadTarget()
                }
                .setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }
                .show()
    }
}

/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-20
 */

package pl.szczodrzynski.edziennik.ui.dialogs

import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.databinding.DialogBellSyncTimeChooseBinding
import pl.szczodrzynski.edziennik.ui.base.dialog.BindingDialog
import pl.szczodrzynski.edziennik.ui.base.dialog.SimpleDialog
import pl.szczodrzynski.edziennik.utils.TextInputDropDown
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time

class BellSyncTimeChooseDialog(
    activity: AppCompatActivity,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BindingDialog<DialogBellSyncTimeChooseBinding>(activity, onShowListener, onDismissListener) {
    companion object {
        private const val MAX_DIFF_MINUTES = 10
    }

    override val TAG = "BellSyncTimeChooseDialog"

    override fun getTitleRes() = R.string.bell_sync_title
    override fun inflate(layoutInflater: LayoutInflater) =
        DialogBellSyncTimeChooseBinding.inflate(layoutInflater)

    override fun getPositiveButtonText() = R.string.ok
    override fun getNeutralButtonText() = R.string.reset
    override fun getNegativeButtonText() = R.string.cancel

    private val today = Date.getToday()
    private val selectedTime: Time?
        get() = b.timeDropdown.selected?.tag as Time?

    override suspend fun onPositiveClick(): Boolean {
        selectedTime?.let {
            BellSyncDialog(activity, it).show()
        }
        return DISMISS
    }

    override suspend fun onNeutralClick(): Boolean {
        SimpleDialog<Unit>(activity) {
            title(R.string.bell_sync_title)
            message(R.string.bell_sync_reset_confirm)
            positive(R.string.yes) {
                app.config.timetable.bellSyncDiff = null
                app.config.timetable.bellSyncMultiplier = 0
                this@BellSyncTimeChooseDialog.dismiss()
                this@BellSyncTimeChooseDialog.show()
                if (activity is MainActivity)
                    activity.reloadTarget()
            }
            negative(R.string.no)
        }.show()
        return NO_DISMISS
    }

    override suspend fun onBeforeShow(): Boolean {
        b.bellSyncHowto.text = app.getString(R.string.bell_sync_choose_howto)

        app.config.timetable.bellSyncDiff?.let { bellDiff ->
            val multiplier = app.config.timetable.bellSyncMultiplier
            val bellDiffText = (if (multiplier == -1) '-' else '+') + bellDiff.stringHMS
            b.bellSyncHowto.text = app.getString(R.string.concat_2_strings,
                app.getString(R.string.bell_sync_choose_howto),
                app.getString(R.string.bell_sync_current_dialog, bellDiffText)
            )
        }

        return loadTimeList()
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

    private suspend fun loadTimeList(): Boolean {
        val timeItems = withContext(Dispatchers.Default) {
            val lessons = app.db.timetableDao().getAllForDateNow(App.profileId, today)
            val items = mutableListOf<TextInputDropDown.Item>()

            lessons.forEach {
                if (it.type != Lesson.TYPE_NO_LESSONS &&
                    it.type != Lesson.TYPE_CANCELLED &&
                    it.type != Lesson.TYPE_SHIFTED_SOURCE
                ) {

                    items += TextInputDropDown.Item(
                        it.displayStartTime?.value?.toLong() ?: return@forEach,
                        app.getString(R.string.bell_sync_lesson_item,
                            it.displaySubjectName,
                            it.displayStartTime?.stringHM),
                        tag = it.displayStartTime
                    )

                    items += TextInputDropDown.Item(
                        it.displayEndTime?.value?.toLong() ?: return@forEach,
                        app.getString(R.string.bell_sync_break_item,
                            it.displayEndTime?.stringHM),
                        tag = it.displayEndTime
                    )
                }
            }

            items
        }

        if (!checkForLessons(timeItems.map { it.tag as Time })) {
            /* Synchronization not possible */
            SimpleDialog<Unit>(activity) {
                title(R.string.bell_sync_title)
                message(R.string.bell_sync_cannot_now)
                positive(R.string.ok)
            }.show()
            return false
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
        }
        return true
    }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-16.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.day

import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.row_lesson_change_item.view.*
import kotlinx.android.synthetic.main.row_teacher_absence_item.view.*
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.databinding.DialogDayBinding
import pl.szczodrzynski.edziennik.ui.dialogs.event.EventDetailsDialog
import pl.szczodrzynski.edziennik.ui.dialogs.event.EventListAdapter
import pl.szczodrzynski.edziennik.ui.dialogs.event.EventManualDialog
import pl.szczodrzynski.edziennik.ui.dialogs.lessonchange.LessonChangeDialog
import pl.szczodrzynski.edziennik.ui.dialogs.teacherabsence.TeacherAbsenceDialog
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.edziennik.utils.models.Week
import kotlin.coroutines.CoroutineContext

class DayDialog(
        val activity: AppCompatActivity,
        val profileId: Int,
        val date: Date,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {
    companion object {
        private const val TAG = "DayDialog"
    }

    private lateinit var app: App
    private lateinit var b: DialogDayBinding
    private lateinit var dialog: AlertDialog

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private lateinit var adapter: EventListAdapter

    init { run {
        if (activity.isFinishing)
            return@run
        onShowListener?.invoke(TAG)
        app = activity.applicationContext as App
        b = DialogDayBinding.inflate(activity.layoutInflater)
        dialog = MaterialAlertDialogBuilder(activity)
                .setView(b.root)
                .setPositiveButton(R.string.close) { dialog, _ ->
                    dialog.dismiss()
                }
                .setNeutralButton(R.string.add, null)
                .setOnDismissListener {
                    onDismissListener?.invoke(TAG)
                }
                .show()

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.onClick {
            EventManualDialog(
                    activity,
                    profileId,
                    defaultDate = date,
                    onShowListener = onShowListener,
                    onDismissListener = onDismissListener
            )
        }

        update()
    }}

    private fun update() { launch {
        b.dayDate.setText(
                R.string.dialog_day_date_format,
                Week.getFullDayName(date.weekDay),
                date.formattedString
        )

        val lessons = withContext(Dispatchers.Default) {
            app.db.timetableDao().getForDateNow(profileId, date)
        }.filter { it.type != Lesson.TYPE_NO_LESSONS }

        if (lessons.isNotEmpty()) { run {
            val startTime = lessons.first().startTime ?: return@run
            val endTime = lessons.last().endTime ?: return@run
            val diff = Time.diff(startTime, endTime)

            b.lessonsInfo.setText(
                    R.string.dialog_day_lessons_info,
                    startTime.stringHM,
                    endTime.stringHM,
                    lessons.size.toString(),
                    diff.hour.toString(),
                    diff.minute.toString()
            )

            b.lessonsInfo.visibility = View.VISIBLE
        }}

        val lessonChanges = withContext(Dispatchers.Default) {
            app.db.timetableDao().getChangesForDateNow(profileId, date)
        }

        lessonChanges.ifNotEmpty {
            b.lessonChangeContainer.visibility = View.VISIBLE
            b.lessonChangeContainer.lessonChangeCount.text = it.size.toString()

            b.lessonChangeLayout.onClick {
                LessonChangeDialog(
                        activity,
                        profileId,
                        date,
                        onShowListener = onShowListener,
                        onDismissListener = onDismissListener
                )
            }
        }

        val teacherAbsences = withContext(Dispatchers.Default) {
            app.db.teacherAbsenceDao().getAllByDateNow(profileId, date)
        }

        teacherAbsences.ifNotEmpty {
            b.teacherAbsenceContainer.visibility = View.VISIBLE
            b.teacherAbsenceContainer.teacherAbsenceCount.text = it.size.toString()

            b.teacherAbsenceLayout.onClick {
                TeacherAbsenceDialog(
                        activity,
                        profileId,
                        date,
                        onShowListener = onShowListener,
                        onDismissListener = onDismissListener
                )
            }
        }

        adapter = EventListAdapter(
                activity,
                onItemClick = {
                    EventDetailsDialog(
                            activity,
                            it,
                            onShowListener = onShowListener,
                            onDismissListener = onDismissListener
                    )
                },
                onEventEditClick = {
                    EventManualDialog(
                            activity,
                            it.profileId,
                            editingEvent = it,
                            onShowListener = onShowListener,
                            onDismissListener = onDismissListener
                    )
                }
        )

        app.db.eventDao().getAllByDate(profileId, date).observe(activity, Observer { events ->
            adapter.items = events
            if (b.eventsView.adapter == null) {
                b.eventsView.adapter = adapter
                b.eventsView.apply {
                    isNestedScrollingEnabled = false
                    setHasFixedSize(true)
                    layoutManager = LinearLayoutManager(context)
                    addItemDecoration(SimpleDividerItemDecoration(context))
                }
            }
            adapter.notifyDataSetChanged()

            if (events != null && events.isNotEmpty()) {
                b.eventsView.visibility = View.VISIBLE
                b.eventsNoData.visibility = View.GONE
            } else {
                b.eventsView.visibility = View.GONE
                b.eventsNoData.visibility = View.VISIBLE
            }
        })
    }}
}

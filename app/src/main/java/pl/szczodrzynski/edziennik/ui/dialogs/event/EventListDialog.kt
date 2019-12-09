/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-30
 */

package pl.szczodrzynski.edziennik.ui.dialogs.event

import android.graphics.Typeface
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.modules.timetable.Lesson
import pl.szczodrzynski.edziennik.data.db.modules.timetable.LessonFull
import pl.szczodrzynski.edziennik.databinding.DialogEventListBinding
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import kotlin.coroutines.CoroutineContext

class EventListDialog(
        val activity: AppCompatActivity,
        val profileId: Int,
        val date: Date,
        val time: Time? = null,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {

    companion object {
        const val TAG = "EventListDialog"
    }

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private val app by lazy { activity.application as App }
    private lateinit var b: DialogEventListBinding
    private lateinit var dialog: AlertDialog
    private lateinit var adapter: EventListAdapter

    private var lesson: LessonFull? = null

    init {
        run {
            if (activity.isFinishing)
                return@run
            job = Job()
            onShowListener?.invoke(TAG)
            b = DialogEventListBinding.inflate(activity.layoutInflater)

            dialog = MaterialAlertDialogBuilder(activity)
                    .setTitle(date.formattedString + (time?.let { ", " + it.stringHM } ?: ""))
                    .setView(b.root)
                    .setPositiveButton(R.string.close) { dialog, _ -> dialog.dismiss() }
                    .setNeutralButton(R.string.add) { _, _ ->
                        EventManualDialog(
                                activity,
                                lesson?.profileId ?: profileId,
                                lesson,
                                date,
                                time,
                                onShowListener = onShowListener,
                                onDismissListener = onDismissListener
                        )
                    }
                    .setOnDismissListener {
                        onDismissListener?.invoke(TAG)
                    }
                    .show()

            app.db.timetableDao().getForDate(profileId, date).observe(activity, Observer { lessons ->
                lesson = lessons.firstOrNull { it.displayStartTime == time }
                update()
            })
        }
    }

    fun dismiss() = dialog.dismiss()

    private fun update() {
        b.eventListLessonDetails.visibility = if (lesson == null) View.GONE else View.VISIBLE

        if (lesson != null) {
            dialog.setTitle(if (time == null) date.formattedString else (lesson?.displaySubjectName
                    ?: date.formattedString) + ", " + time.stringHM)

            b.eventListLessonDate.text = app.getString(R.string.date_time_format, date.formattedString, "")

            if (lesson?.type == Lesson.TYPE_CANCELLED) {
                b.eventListLessonChange.text = app.getString(R.string.lesson_cancelled)
                b.eventListLessonChange.setTypeface(null, Typeface.BOLD_ITALIC)
                b.eventListTeacher.visibility = View.GONE
                b.eventListClassroom.visibility = View.GONE
            } else {
                b.eventListLessonChange.text = lesson?.changeSubjectName
                b.eventListLessonChange.setTypeface(null, Typeface.ITALIC)
                b.eventListLessonChange.visibility = if (lesson?.isSubjectNameChanged == true) View.VISIBLE else View.GONE

                b.eventListTeacher.text = lesson?.changeTeacherName
                b.eventListTeacher.setTypeface(null, if (lesson?.isTeacherNameChanged == true) Typeface.ITALIC else Typeface.NORMAL)

                b.eventListClassroom.text = lesson?.changeClassroom
                b.eventListClassroom.setTypeface(null, if (lesson?.isClassroomChanged == true) Typeface.ITALIC else Typeface.NORMAL)
            }
        }

        b.eventListView.apply {
            setHasFixedSize(false)
            isNestedScrollingEnabled = true
            layoutManager = LinearLayoutManager(activity)
        }

        adapter = EventListAdapter(activity, this@EventListDialog)
        b.eventListView.adapter = adapter

        app.db.eventDao().getAllByDateTime(profileId, date, time).observe(activity, Observer { events ->
            if (events.isNullOrEmpty()) {
                b.eventListView.visibility = View.GONE
                b.textNoEvents.visibility = View.VISIBLE
            } else {
                adapter.run {
                    eventList.apply {
                        clear()
                        addAll(events)
                    }
                    notifyDataSetChanged()
                }
            }
        })
    }
}

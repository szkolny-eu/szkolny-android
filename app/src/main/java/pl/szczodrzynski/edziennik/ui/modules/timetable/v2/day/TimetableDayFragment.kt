package pl.szczodrzynski.edziennik.ui.modules.timetable.v2.day

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.linkedin.android.tachyon.DayView
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.api.v2.events.task.EdziennikTask
import pl.szczodrzynski.edziennik.data.db.modules.timetable.Lesson
import pl.szczodrzynski.edziennik.data.db.modules.timetable.LessonFull
import pl.szczodrzynski.edziennik.databinding.FragmentTimetableV2DayBinding
import pl.szczodrzynski.edziennik.databinding.TimetableLessonBinding
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.navlib.getColorFromAttr
import java.util.*

class TimetableDayFragment(val date: Date) : Fragment() {
    companion object {
        private const val TAG = "TimetableDayFragment"
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: FragmentTimetableV2DayBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        if (context == null)
            return null
        app = activity.application as App
        b = FragmentTimetableV2DayBinding.inflate(inflater)
        Log.d(TAG, "onCreateView, date=$date")
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // TODO check if app, activity, b can be null
        if (app.profile == null || !isAdded)
            return

        Log.d(TAG, "onViewCreated, date=$date")

        // Inflate a label view for each hour the day view will display
        val hourLabelViews = ArrayList<View>()
        for (i in b.day.startHour..b.day.endHour) {
            val hourLabelView = layoutInflater.inflate(R.layout.timetable_hour_label, b.day, false) as TextView
            hourLabelView.text = "$i:00"
            hourLabelViews.add(hourLabelView)
        }
        b.day.setHourLabelViews(hourLabelViews)

        app.db.timetableDao().getForDate(App.profileId, date).observe(this, Observer<List<LessonFull>> { lessons ->
            buildLessonViews(lessons)
        })
    }

    private fun buildLessonViews(lessons: List<LessonFull>) {
        if (lessons.isEmpty()) {
            b.dayScroll.visibility = View.GONE
            b.noTimetableLayout.visibility = View.VISIBLE
            b.noLessonsLayout.visibility = View.GONE
            b.noTimetableSync.setOnClickListener {
                EdziennikTask.syncProfile(
                        profileId = App.profileId,
                        arguments = JsonObject(
                                "weekStart" to date.clone().stepForward(0, 0, -date.weekDay).stringY_m_d
                        )
                ).enqueue(activity)
            }
            return
        }
        if (lessons.size == 1 && lessons[0].type == Lesson.TYPE_NO_LESSONS) {
            b.dayScroll.visibility = View.GONE
            b.noTimetableLayout.visibility = View.GONE
            b.noLessonsLayout.visibility = View.VISIBLE
            return
        }

        b.dayScroll.visibility = View.VISIBLE
        b.noTimetableLayout.visibility = View.GONE
        b.noLessonsLayout.visibility = View.GONE

        val eventViews = mutableListOf<View>()
        val eventTimeRanges = mutableListOf<DayView.EventTimeRange>()

        // Reclaim all of the existing event views so we can reuse them if needed, this process
        // can be useful if your day view is hosted in a recycler view for example
        val recycled = b.day.removeEventViews()
        var remaining = recycled?.size ?: 0

        val arrowRight = " → "
        val bullet = " • "
        val colorSecondary = getColorFromAttr(activity, android.R.attr.textColorSecondary)

        for (lesson in lessons) {
            val startTime = lesson.displayStartTime ?: continue
            val endTime = lesson.displayEndTime ?: continue

            // Try to recycle an existing event view if there are enough left, otherwise inflate
            // a new one
            val eventView = (if (remaining > 0) recycled?.get(--remaining) else layoutInflater.inflate(R.layout.timetable_lesson, b.day, false))
                    ?: continue
            val lb = TimetableLessonBinding.bind(eventView)
            eventViews += eventView

            eventView.tag = lesson

            eventView.setOnClickListener {
                Log.d(TAG, "Clicked ${it.tag}")
            }


            val timeRange = "${startTime.stringHM} - ${endTime.stringHM}".asColoredSpannable(colorSecondary)

            // teacher
            val teacherInfo = if (lesson.teacherId != null && lesson.teacherId == lesson.oldTeacherId)
                lesson.teacherName ?: "?"
            else
                mutableListOf<CharSequence>().apply {
                    lesson.oldTeacherName?.let { add(it.asStrikethroughSpannable()) }
                    lesson.teacherName?.let { add(it) }
                }.concat(arrowRight)

            // team
            val teamInfo = if (lesson.teamId != null && lesson.teamId == lesson.oldTeamId)
                lesson.teamName ?: "?"
            else
                mutableListOf<CharSequence>().apply {
                    lesson.oldTeamName?.let { add(it.asStrikethroughSpannable()) }
                    lesson.teamName?.let { add(it) }
                }.concat(arrowRight)

            // classroom
            val classroomInfo = if (lesson.classroom != null && lesson.classroom == lesson.oldClassroom)
                lesson.classroom ?: "?"
            else
                mutableListOf<CharSequence>().apply {
                    lesson.oldClassroom?.let { add(it.asStrikethroughSpannable()) }
                    lesson.classroom?.let { add(it) }
                }.concat(arrowRight)


            lb.lessonNumber = lesson.displayLessonNumber
            lb.subjectName.text = lesson.displaySubjectName?.let {
                if (lesson.type == Lesson.TYPE_CANCELLED || lesson.type == Lesson.TYPE_SHIFTED_SOURCE)
                    it.asStrikethroughSpannable().asColoredSpannable(colorSecondary)
                else
                    it
            }
            lb.detailsFirst.text = listOfNotEmpty(timeRange, classroomInfo).concat(bullet)
            lb.detailsSecond.text = listOfNotEmpty(teacherInfo, teamInfo).concat(bullet)

            //lb.subjectName.typeface = Typeface.create("sans-serif-light", Typeface.BOLD)
            when (lesson.type) {
                Lesson.TYPE_NORMAL -> {
                    lb.annotationVisible = false
                }
                Lesson.TYPE_CANCELLED -> {
                    lb.annotationVisible = true
                    lb.annotation.setText(R.string.timetable_lesson_cancelled)
                    lb.annotation.background.colorFilter = PorterDuffColorFilter(
                            getColorFromAttr(activity, R.attr.timetable_lesson_cancelled_color),
                            PorterDuff.Mode.SRC_ATOP
                    )
                    //lb.subjectName.typeface = Typeface.DEFAULT
                }
                Lesson.TYPE_CHANGE -> {
                    lb.annotationVisible = true
                    if (lesson.subjectId != lesson.oldSubjectId && lesson.teacherId != lesson.oldTeacherId) {
                        lb.annotation.setText(
                                R.string.timetable_lesson_change_format,
                                "${lesson.oldSubjectName ?: "?"}, ${lesson.oldTeacherName ?: "?"}"
                        )
                    }
                    else if (lesson.subjectId != lesson.oldSubjectId) {
                        lb.annotation.setText(
                                R.string.timetable_lesson_change_format,
                                lesson.oldSubjectName ?: "?"
                        )
                    }
                    else if (lesson.teacherId != lesson.oldTeacherId) {
                        lb.annotation.setText(
                                R.string.timetable_lesson_change_format,
                                lesson.oldTeacherName ?: "?"
                        )
                    }
                    else {
                        lb.annotation.setText(R.string.timetable_lesson_change)
                    }

                    lb.annotation.background.colorFilter = PorterDuffColorFilter(
                            getColorFromAttr(activity, R.attr.timetable_lesson_change_color),
                            PorterDuff.Mode.SRC_ATOP
                    )
                }
                Lesson.TYPE_SHIFTED_SOURCE -> {
                    lb.annotationVisible = true
                    if (lesson.date != lesson.oldDate) {
                        lb.annotation.setText(
                                R.string.timetable_lesson_shifted_other_day,
                                lesson.date?.stringY_m_d ?: "?",
                                lesson.startTime?.stringHM ?: "?"
                        )
                    }
                    else if (lesson.startTime != lesson.oldStartTime) {
                        lb.annotation.setText(
                                R.string.timetable_lesson_shifted_same_day,
                                lesson.startTime?.stringHM ?: "?"
                        )
                    }
                    else {
                        lb.annotation.setText(R.string.timetable_lesson_shifted)
                    }

                    lb.annotation.background.colorFilter = PorterDuffColorFilter(
                            getColorFromAttr(activity, R.attr.timetable_lesson_shifted_source_color),
                            PorterDuff.Mode.SRC_ATOP
                    )
                }
                Lesson.TYPE_SHIFTED_TARGET -> {
                    lb.annotationVisible = true
                    if (lesson.date != lesson.oldDate) {
                        lb.annotation.setText(
                                R.string.timetable_lesson_shifted_from_other_day,
                                lesson.oldDate?.stringY_m_d ?: "?",
                                lesson.oldStartTime?.stringHM ?: "?"
                        )
                    }
                    else if (lesson.startTime != lesson.oldStartTime) {
                        lb.annotation.setText(
                                R.string.timetable_lesson_shifted_from_same_day,
                                lesson.oldStartTime?.stringHM ?: "?"
                        )
                    }
                    else {
                        lb.annotation.setText(R.string.timetable_lesson_shifted_from)
                    }

                    lb.annotation.background.colorFilter = PorterDuffColorFilter(
                            getColorFromAttr(activity, R.attr.timetable_lesson_shifted_target_color),
                            PorterDuff.Mode.SRC_ATOP
                    )
                }
            }


            // The day view needs the event time ranges in the start minute/end minute format,
            // so calculate those here
            val startMinute = 60 * (lesson.displayStartTime?.hour ?: 0) + (lesson.displayStartTime?.minute ?: 0)
            val endMinute = startMinute + 45
            eventTimeRanges.add(DayView.EventTimeRange(startMinute, endMinute))
        }

        b.day.setEventViews(eventViews, eventTimeRanges)
        b.dayScroll.scrollTo(0, b.day.firstEventTop)
    }
}

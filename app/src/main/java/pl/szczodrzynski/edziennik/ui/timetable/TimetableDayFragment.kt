/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.ui.timetable

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import androidx.core.view.setPadding
import androidx.core.view.updateLayoutParams
import com.linkedin.android.tachyon.DayView
import com.linkedin.android.tachyon.DayViewConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.core.manager.NoteManager
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.db.full.AttendanceFull
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.data.db.full.LessonFull
import pl.szczodrzynski.edziennik.data.enums.FeatureType
import pl.szczodrzynski.edziennik.databinding.TimetableDayFragmentBinding
import pl.szczodrzynski.edziennik.databinding.TimetableLessonBinding
import pl.szczodrzynski.edziennik.databinding.TimetableNoLessonsBinding
import pl.szczodrzynski.edziennik.databinding.TimetableNoTimetableBinding
import pl.szczodrzynski.edziennik.ext.Intent
import pl.szczodrzynski.edziennik.ext.JsonObject
import pl.szczodrzynski.edziennik.ext.asColoredSpannable
import pl.szczodrzynski.edziennik.ext.asStrikethroughSpannable
import pl.szczodrzynski.edziennik.ext.concat
import pl.szczodrzynski.edziennik.ext.dp
import pl.szczodrzynski.edziennik.ext.getStudentData
import pl.szczodrzynski.edziennik.ext.listOfNotEmpty
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ext.resolveAttr
import pl.szczodrzynski.edziennik.ext.resolveDrawable
import pl.szczodrzynski.edziennik.ext.setText
import pl.szczodrzynski.edziennik.ext.setTintColor
import pl.szczodrzynski.edziennik.ext.startCoroutineTimer
import pl.szczodrzynski.edziennik.ext.toDrawable
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.ui.timetable.TimetableFragment.Companion.DEFAULT_END_HOUR
import pl.szczodrzynski.edziennik.ui.timetable.TimetableFragment.Companion.DEFAULT_START_HOUR
import pl.szczodrzynski.edziennik.utils.Colors
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.edziennik.utils.models.Week
import pl.szczodrzynski.edziennik.utils.mutableLazy
import kotlin.math.max
import kotlin.math.min

class TimetableDayFragment : BaseFragment<TimetableDayFragmentBinding, MainActivity>(
    inflater = TimetableDayFragmentBinding::inflate,
) {

    private lateinit var inflater: AsyncLayoutInflater

    private var timeIndicatorJob: Job? = null

    private lateinit var date: Date
    private var startHour = DEFAULT_START_HOUR
    private var endHour = DEFAULT_END_HOUR
    private var firstEventMinute = 24 * 60
    private var paddingTop = 0
    private var syncArgs = JsonObject()

    private var viewsRemoved = false

    private val manager
        get() = app.timetableManager
    private val attendanceManager
        get() = app.attendanceManager

    private val dayViewDelegate = mutableLazy {
        val dayView = DayView(
            activity, DayViewConfig(
                startHour = startHour,
                endHour = endHour,
                dividerHeight = 1.dp,
                halfHourHeight = app.data.uiConfig.lessonHeight.dp,
                hourDividerColor = R.attr.hourDividerColor.resolveAttr(context),
                halfHourDividerColor = R.attr.halfHourDividerColor.resolveAttr(context),
                hourLabelWidth = 40.dp,
                hourLabelMarginEnd = 10.dp,
                eventMargin = 2.dp
            ), true
        )
        dayView.setPadding(10.dp)
        return@mutableLazy dayView
    }
    private val dayView by dayViewDelegate

    override fun getScrollingView() = b.scrollView
    override fun getSyncParams() = FeatureType.TIMETABLE to syncArgs

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        this.inflater = AsyncLayoutInflater(requireContext())

        date = arguments?.getInt("date")?.let { Date.fromValue(it) } ?: Date.getToday()
        startHour = arguments?.getInt("startHour") ?: DEFAULT_START_HOUR
        endHour = arguments?.getInt("endHour") ?: DEFAULT_END_HOUR

        syncArgs = JsonObject(
            "weekStart" to date.weekStart.stringY_m_d
        )

        // observe lesson database
        app.db.timetableDao().getAllForDate(App.profileId, date).observe(this) { lessons ->
            launch {
                lessons.forEach {
                    it.filterNotes()
                }

                val events = withContext(Dispatchers.Default) {
                    app.db.eventDao().getAllByDateNow(App.profileId, date)
                }
                val attendanceList = withContext(Dispatchers.Default) {
                    app.db.attendanceDao().getAllByDateNow(App.profileId, date)
                }
                processLessonList(lessons, events, attendanceList)
            }
        }
    }

    private fun processLessonList(
        lessons: List<LessonFull>,
        events: List<EventFull>,
        attendanceList: List<AttendanceFull>,
    ) {
        // no lessons - timetable not downloaded yet
        if (lessons.isEmpty()) {
            inflater.inflate(R.layout.timetable_no_timetable, b.root) { view, _, _ ->
                b.root.removeAllViews()
                b.root.addView(view)
                viewsRemoved = true

                val b = TimetableNoTimetableBinding.bind(view)
                val weekStart = date.weekStart.stringY_m_d
                b.noTimetableSync.onClick {
                    it.isEnabled = false
                    EdziennikTask.syncProfile(
                        profileId = App.profileId,
                        featureTypes = setOf(FeatureType.TIMETABLE),
                        arguments = JsonObject(
                            "weekStart" to weekStart
                        )
                    ).enqueue(activity)
                }
                b.noTimetableWeek.setText(R.string.timetable_no_timetable_week, weekStart)
            }
            return
        }
        // one lesson indicating a day without lessons
        if (lessons.size == 1 && lessons[0].type == Lesson.TYPE_NO_LESSONS) {
            inflater.inflate(R.layout.timetable_no_lessons, b.root) { view, _, _ ->
                b.root.removeAllViews()
                b.root.addView(view)
                viewsRemoved = true

                val b = TimetableNoLessonsBinding.bind(view)
                val weekStart = date.weekStart.stringY_m_d
                b.noLessonsSync.onClick {
                    it.isEnabled = false
                    EdziennikTask.syncProfile(
                        profileId = App.profileId,
                        featureTypes = setOf(FeatureType.TIMETABLE),
                        arguments = JsonObject(
                            "weekStart" to weekStart
                        )
                    ).enqueue(activity)
                }
                b.noLessonsSync.isVisible = date.weekDay !in Week.SATURDAY..Week.SUNDAY
            }
            return
        }

        // reload the fragment when: no lessons, user wants to sync the week, the timetable is not public, pager gets removed
        if (app.profile.getStudentData("timetableNotPublic", false)) {
            activity.reloadTarget()
            // TODO fix for (not really)possible infinite loops
            return
        }

        // the timetable was not synced (the day layout views are removed) and is now available
        if (viewsRemoved) {
            viewsRemoved = false
            activity.sendBroadcast(Intent(TimetableFragment.ACTION_RELOAD_PAGES))
            return
        }

        if (dayViewDelegate.isInitialized())
            b.dayFrame.removeView(dayView)

        val lessonsActual = lessons.filter { it.type != Lesson.TYPE_NO_LESSONS }

        val minStartHour = lessonsActual.minOf { it.displayStartTime?.hour ?: DEFAULT_END_HOUR }
        val maxEndHour =
            lessonsActual.maxOf { it.displayEndTime?.hour?.plus(1) ?: DEFAULT_START_HOUR }

        if (app.profile.config.ui.timetableTrimHourRange) {
            dayViewDelegate.deinitialize()
            // end/start defaults are swapped on purpose
            startHour = minStartHour
            endHour = maxEndHour
        } else if (startHour > minStartHour || endHour < maxEndHour) {
            dayViewDelegate.deinitialize()
            startHour = min(startHour, minStartHour)
            endHour = max(endHour, maxEndHour)
        }

        b.scrollView.isVisible = true
        b.dayFrame.addView(dayView, 0)

        // Inflate a label view for each hour the day view will display
        val hourLabelViews = mutableListOf<View>()
        for (i in dayView.startHour..dayView.endHour) {
            if (!isAdded)
                continue
            val hourLabelView =
                layoutInflater.inflate(R.layout.timetable_hour_label, dayView, false) as TextView
            hourLabelView.text = "$i:00"
            hourLabelViews.add(hourLabelView)
        }
        dayView.setHourLabelViews(hourLabelViews)
        // measure dayView top padding needed for the timeIndicator
        hourLabelViews.getOrNull(0)?.let {
            it.measure(0, 0)
            paddingTop = it.measuredHeight / 2 + dayView.paddingTop
        }

        lessons.forEach { it.showAsUnseen = !it.seen }

        buildLessonViews(lessonsActual, events, attendanceList)
    }

    private fun buildLessonViews(
        lessons: List<LessonFull>,
        events: List<EventFull>,
        attendanceList: List<AttendanceFull>,
    ) {
        if (!isAdded)
            return

        val eventViews = mutableListOf<View>()
        val eventTimeRanges = mutableListOf<DayView.EventTimeRange>()

        // Reclaim all of the existing event views so we can reuse them if needed, this process
        // can be useful if your day view is hosted in a recycler view for example
        val recycled = dayView.removeEventViews()
        var remaining = recycled?.size ?: 0

        val arrowRight = " → "
        val bullet = " • "
        val colorSecondary = android.R.attr.textColorSecondary.resolveAttr(activity)

        for (lesson in lessons) {
            val attendance = if (app.profile.config.ui.timetableShowAttendance)
                attendanceList.find { it.startTime == lesson.startTime }
            else
                null
            val startTime = lesson.displayStartTime ?: continue
            val endTime = lesson.displayEndTime ?: continue

            firstEventMinute = min(firstEventMinute, startTime.hour * 60 + startTime.minute)

            // Try to recycle an existing event view if there are enough left, otherwise inflate
            // a new one
            val eventView =
                (if (remaining > 0) recycled?.get(--remaining) else layoutInflater.inflate(
                    R.layout.timetable_lesson,
                    dayView,
                    false
                )) ?: continue
            val lb = TimetableLessonBinding.bind(eventView)
            eventViews += eventView

            eventView.tag = lesson to attendance

            eventView.setOnClickListener {
                if (isAdded && it.tag is Pair<*, *>) {
                    val (lessonObj, attendanceObj) = it.tag as Pair<*, *>
                    LessonDetailsDialog(
                        activity = activity,
                        lesson = lessonObj as LessonFull,
                        attendance = attendanceObj as AttendanceFull?
                    ).show()
                }
            }

            val eventIcons = listOf(lb.event1, lb.event2, lb.event3)
            if (app.profile.config.ui.timetableShowEvents) {
                val eventList =
                    events.filter { it.time != null && it.time == lesson.displayStartTime }.take(3)
                for ((i, eventIcon) in eventIcons.withIndex()) {
                    eventList.getOrNull(i).let {
                        eventIcon.isVisible = it != null
                        eventIcon.background = it?.let {
                            R.drawable.bg_circle.resolveDrawable(activity)
                                .setTintColor(it.eventColor)
                        }
                    }
                }
            } else {
                for (eventIcon in eventIcons) {
                    eventIcon.visibility = View.GONE
                }
            }


            val timeRange =
                "${startTime.stringHM} - ${endTime.stringHM}".asColoredSpannable(colorSecondary)

            // teacher
            val teacherInfo =
                if (lesson.teacherId != null && lesson.teacherId == lesson.oldTeacherId)
                    lesson.teacherName ?: "?"
                else
                    lesson.teacherName
                    ?: lesson.oldTeacherName?.asStrikethroughSpannable()
                    ?: ""

            // team
            val teamInfo = if (lesson.teamId != null && lesson.teamId == lesson.oldTeamId)
                lesson.teamName ?: "?"
            else
                lesson.teamName
                ?: lesson.oldTeamName?.asStrikethroughSpannable()
                ?: ""

            // classroom
            val classroomInfo =
                if (lesson.classroom != null && lesson.classroom == lesson.oldClassroom)
                    lesson.classroom ?: "?"
                else
                    lesson.classroom
                    ?: lesson.oldClassroom?.asStrikethroughSpannable()
                    ?: ""

            lb.annotationVisible = manager.getAnnotation(activity, lesson, lb.annotation)

            val lessonText =
                lesson.getNoteSubstituteText(showNotes = true) ?: lesson.displaySubjectName

            val (subjectTextPrimary, subjectTextSecondary) = if (app.profile.config.ui.timetableColorSubjectName) {
                val subjectColor =
                    lesson.color ?: Colors.stringToMaterialColorCRC(lessonText?.toString() ?: "")
                if (lb.annotationVisible) {
                    lb.subjectContainer.background = ColorDrawable(subjectColor)
                } else {
                    lb.subjectContainer.setBackgroundResource(R.drawable.timetable_subject_color_rounded)
                    lb.subjectContainer.background.setTintColor(subjectColor)
                }
                when (ColorUtils.calculateLuminance(subjectColor) > 0.5) {
                    true -> /* light */ 0xFF000000 to 0xFF666666
                    false -> /* dark */ 0xFFFFFFFF to 0xFFAAAAAA
                }
            } else {
                lb.subjectContainer.background = null
                null to colorSecondary
            }

            lb.lessonNumber = lesson.displayLessonNumber
            if (subjectTextPrimary != null)
                lb.lessonNumberText.setTextColor(subjectTextPrimary.toInt())
            lb.subjectName.text = lessonText?.let {
                if (lesson.type == Lesson.TYPE_CANCELLED || lesson.type == Lesson.TYPE_SHIFTED_SOURCE)
                    it.asStrikethroughSpannable().asColoredSpannable(subjectTextSecondary.toInt())
                else if (subjectTextPrimary != null)
                    it.asColoredSpannable(subjectTextPrimary.toInt())
                else
                    it
            }
            lb.detailsFirst.text = listOfNotEmpty(timeRange, classroomInfo).concat(bullet)
            lb.detailsSecond.text = listOfNotEmpty(teacherInfo, teamInfo).concat(bullet)

            NoteManager.prependIcon(lesson, lb.subjectName)

            lb.attendanceIcon.isVisible = attendance?.let {
                val icon = attendanceManager.getAttendanceIcon(it) ?: return@let false
                val color = attendanceManager.getAttendanceColor(it)
                lb.attendanceIcon.setImageDrawable(icon.toDrawable(color))
                true
            } ?: false

            lb.unread = lesson.type != Lesson.TYPE_NORMAL && lesson.showAsUnseen
            if (!lesson.seen) {
                manager.markAsSeen(lesson)
            }

            //lb.subjectName.typeface = Typeface.create("sans-serif-light", Typeface.BOLD)
            // TODO bring it back on classic theme
            /*val lessonNumberMargin =
                if (lb.annotationVisible) (-8).dp
                else 0
            lb.lessonNumberText.updateLayoutParams<LinearLayout.LayoutParams> {
                updateMargins(top = lessonNumberMargin, bottom = lessonNumberMargin)
            }*/

            // The day view needs the event time ranges in the start minute/end minute format,
            // so calculate those here
            val startMinute = 60 * startTime.hour + startTime.minute
            val endMinute = 60 * endTime.hour + endTime.minute
            eventTimeRanges.add(DayView.EventTimeRange(startMinute, endMinute))
        }

        updateTimeIndicator()

        dayView.setEventViews(eventViews, eventTimeRanges)
        val firstEventTop = (firstEventMinute - dayView.startHour * 60) * dayView.minuteHeight
        b.scrollView.scrollTo(0, firstEventTop.toInt())

        b.progressBar.isVisible = false
    }

    private fun updateTimeIndicator() {
        val time = Time.getNow()
        val isTimeInView =
            date == Date.getToday() && time.hour in dayView.startHour..dayView.endHour

        b.timeIndicator.isVisible = isTimeInView
        b.timeIndicatorMarker.isVisible = isTimeInView
        if (isTimeInView) {
            val startTime = Time(dayView.startHour, 0, 0)
            val seconds = time.inSeconds - startTime.inSeconds * 1f
            b.timeIndicator.updateLayoutParams<FrameLayout.LayoutParams> {
                topMargin = (seconds * dayView.minuteHeight / 60f).toInt() + paddingTop
            }
            b.timeIndicatorMarker.updateLayoutParams<FrameLayout.LayoutParams> {
                topMargin = b.timeIndicator.marginTop - (16.dp / 2) + (1.dp / 2)
            }
        }

        if (timeIndicatorJob == null) {
            timeIndicatorJob = startCoroutineTimer(repeatMillis = 30000) {
                updateTimeIndicator()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!::date.isInitialized)
            return
        val firstEventTop = (firstEventMinute - dayView.startHour * 60) * dayView.minuteHeight
        b.scrollView.scrollTo(0, firstEventTop.toInt())
        updateTimeIndicator()
    }

    override fun onPause() {
        super.onPause()
        timeIndicatorJob?.cancel()
        timeIndicatorJob = null
    }
}

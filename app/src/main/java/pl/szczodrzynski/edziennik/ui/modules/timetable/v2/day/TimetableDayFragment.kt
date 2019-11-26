package pl.szczodrzynski.edziennik.ui.modules.timetable.v2.day

import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.linkedin.android.tachyon.DayView
import com.linkedin.android.tachyon.DayViewConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.MainActivity.Companion.DRAWER_ITEM_TIMETABLE
import pl.szczodrzynski.edziennik.api.v2.LOGIN_TYPE_LIBRUS
import pl.szczodrzynski.edziennik.api.v2.events.task.EdziennikTask
import pl.szczodrzynski.edziennik.data.db.modules.timetable.Lesson
import pl.szczodrzynski.edziennik.data.db.modules.timetable.LessonFull
import pl.szczodrzynski.edziennik.databinding.TimetableLessonBinding
import pl.szczodrzynski.edziennik.databinding.TimetableNoTimetableBinding
import pl.szczodrzynski.edziennik.ui.dialogs.timetable.LessonDetailsDialog
import pl.szczodrzynski.edziennik.ui.modules.timetable.v2.TimetableFragment.Companion.DEFAULT_END_HOUR
import pl.szczodrzynski.edziennik.ui.modules.timetable.v2.TimetableFragment.Companion.DEFAULT_START_HOUR
import pl.szczodrzynski.edziennik.utils.ListenerScrollView
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.navlib.getColorFromAttr
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.min

class TimetableDayFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "TimetableDayFragment"
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var inflater: AsyncLayoutInflater

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private lateinit var date: Date
    private var startHour = DEFAULT_START_HOUR
    private var endHour = DEFAULT_END_HOUR
    private var firstEventMinute = 24 * 60

    // find SwipeRefreshLayout in the hierarchy
    private val refreshLayout by lazy { view?.findParentById(R.id.refreshLayout) }
    // the day ScrollView
    private val dayScrollDelegate = lazy {
        val dayScroll = ListenerScrollView(context!!)
        dayScroll.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dayScroll.setOnRefreshLayoutEnabledListener { enabled ->
            refreshLayout?.isEnabled = enabled
        }
        dayScroll
    }
    private val dayScroll by dayScrollDelegate
    // the lesson DayView
    private val dayView by lazy {
        val dayView = DayView(context!!, DayViewConfig(
                startHour = startHour,
                endHour = endHour,
                dividerHeight = 1.dp,
                halfHourHeight = 60.dp,
                hourDividerColor = R.attr.hourDividerColor.resolveAttr(context),
                halfHourDividerColor = R.attr.halfHourDividerColor.resolveAttr(context),
                hourLabelWidth = 40.dp,
                hourLabelMarginEnd = 10.dp,
                eventMargin = 2.dp
        ), true)
        dayView.setPadding(10.dp)
        dayScroll.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        dayScroll.addView(dayView)
        dayView
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        if (context == null)
            return null
        app = activity.application as App
        job = Job()
        this.inflater = AsyncLayoutInflater(context!!)
        date = arguments?.getInt("date")?.let { Date.fromValue(it) } ?: Date.getToday()
        startHour = arguments?.getInt("startHour") ?: DEFAULT_START_HOUR
        endHour = arguments?.getInt("endHour") ?: DEFAULT_END_HOUR
        return FrameLayout(activity).apply {
            layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // TODO check if app, activity, b can be null
        if (app.profile == null || !isAdded)
            return

        Log.d(TAG, "onViewCreated, date=$date")

        // observe lesson database
        app.db.timetableDao().getForDate(App.profileId, date).observe(this, Observer<List<LessonFull>> { lessons ->
            processLessonList(lessons)
        })
    }

    private fun processLessonList(lessons: List<LessonFull>) {
        // no lessons - timetable not downloaded yet
        if (lessons.isEmpty()) {
            inflater.inflate(R.layout.timetable_no_timetable, view as FrameLayout) { view, _, parent ->
                parent?.removeAllViews()
                parent?.addView(view)
                val b = TimetableNoTimetableBinding.bind(view)
                val weekStart = date.weekStart.stringY_m_d
                b.noTimetableSync.onClick {
                    it.isEnabled = false
                    EdziennikTask.syncProfile(
                            profileId = App.profileId,
                            viewIds = listOf(
                                    DRAWER_ITEM_TIMETABLE to 0
                            ),
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
            inflater.inflate(R.layout.timetable_no_lessons, view as FrameLayout) { view, _, parent ->
                parent?.removeAllViews()
                parent?.addView(view)
            }
            return
        }

        // reload the fragment when: no lessons, user wants to sync the week, the timetable is not public, pager gets removed
        if (app.profile.loginStoreType == LOGIN_TYPE_LIBRUS && app.profile.getLoginData("timetableNotPublic", false)) {
            activity.reloadTarget()
            // TODO fix for (not really)possible infinite loops
            return
        }

        // clear the root view and add the ScrollView
        (view as FrameLayout).removeAllViews()
        (view as FrameLayout).addView(dayScroll)

        // Inflate a label view for each hour the day view will display
        val hourLabelViews = ArrayList<View>()
        for (i in dayView.startHour..dayView.endHour) {
            val hourLabelView = layoutInflater.inflate(R.layout.timetable_hour_label, dayView, false) as TextView
            hourLabelView.text = "$i:00"
            hourLabelViews.add(hourLabelView)
        }
        dayView.setHourLabelViews(hourLabelViews)

        buildLessonViews(lessons)
    }

    private fun buildLessonViews(lessons: List<LessonFull>) {
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
            val startTime = lesson.displayStartTime ?: continue
            val endTime = lesson.displayEndTime ?: continue

            firstEventMinute = min(firstEventMinute, startTime.hour * 60 + startTime.minute)

            // Try to recycle an existing event view if there are enough left, otherwise inflate
            // a new one
            val eventView = (if (remaining > 0) recycled?.get(--remaining) else layoutInflater.inflate(R.layout.timetable_lesson, dayView, false))
                    ?: continue
            val lb = TimetableLessonBinding.bind(eventView)
            eventViews += eventView

            eventView.tag = lesson

            eventView.setOnClickListener {
                Log.d(TAG, "Clicked ${it.tag}")
                if (isAdded && it.tag is LessonFull)
                    LessonDetailsDialog(activity, it.tag as LessonFull)
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

            lb.unread = lesson.type != Lesson.TYPE_NORMAL && !lesson.seen

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
                    when {
                        lesson.subjectId != lesson.oldSubjectId && lesson.teacherId != lesson.oldTeacherId
                                && lesson.oldSubjectName != null && lesson.oldTeacherName != null ->
                            lb.annotation.setText(
                                    R.string.timetable_lesson_change_format,
                                    "${lesson.oldSubjectName ?: "?"}, ${lesson.oldTeacherName ?: "?"}"
                            )

                        lesson.subjectId != lesson.oldSubjectId && lesson.oldSubjectName != null ->
                            lb.annotation.setText(
                                    R.string.timetable_lesson_change_format,
                                    lesson.oldSubjectName ?: "?"
                            )

                        lesson.teacherId != lesson.oldTeacherId && lesson.oldTeacherName != null ->
                            lb.annotation.setText(
                                    R.string.timetable_lesson_change_format,
                                    lesson.oldTeacherName ?: "?"
                            )
                        else -> lb.annotation.setText(R.string.timetable_lesson_change)
                    }

                    lb.annotation.background.colorFilter = PorterDuffColorFilter(
                            getColorFromAttr(activity, R.attr.timetable_lesson_change_color),
                            PorterDuff.Mode.SRC_ATOP
                    )
                }
                Lesson.TYPE_SHIFTED_SOURCE -> {
                    lb.annotationVisible = true
                    when {
                        lesson.date != lesson.oldDate && lesson.date != null ->
                            lb.annotation.setText(
                                    R.string.timetable_lesson_shifted_other_day,
                                    lesson.date?.stringY_m_d ?: "?",
                                    lesson.startTime?.stringHM ?: ""
                            )

                        lesson.startTime != lesson.oldStartTime && lesson.startTime != null ->
                            lb.annotation.setText(
                                    R.string.timetable_lesson_shifted_same_day,
                                    lesson.startTime?.stringHM ?: "?"
                            )

                        else -> lb.annotation.setText(R.string.timetable_lesson_shifted)
                    }

                    lb.annotation.background.colorFilter = PorterDuffColorFilter(
                            getColorFromAttr(activity, R.attr.timetable_lesson_shifted_source_color),
                            PorterDuff.Mode.SRC_ATOP
                    )
                }
                Lesson.TYPE_SHIFTED_TARGET -> {
                    lb.annotationVisible = true
                    when {
                        lesson.date != lesson.oldDate && lesson.oldDate != null ->
                            lb.annotation.setText(
                                    R.string.timetable_lesson_shifted_from_other_day,
                                    lesson.oldDate?.stringY_m_d ?: "?",
                                    lesson.oldStartTime?.stringHM ?: ""
                            )

                        lesson.startTime != lesson.oldStartTime && lesson.oldStartTime != null ->
                            lb.annotation.setText(
                                    R.string.timetable_lesson_shifted_from_same_day,
                                    lesson.oldStartTime?.stringHM ?: "?"
                            )

                        else -> lb.annotation.setText(R.string.timetable_lesson_shifted_from)
                    }

                    lb.annotation.background.colorFilter = PorterDuffColorFilter(
                            getColorFromAttr(activity, R.attr.timetable_lesson_shifted_target_color),
                            PorterDuff.Mode.SRC_ATOP
                    )
                }
            }


            // The day view needs the event time ranges in the start minute/end minute format,
            // so calculate those here
            val startMinute = 60 * (lesson.displayStartTime?.hour
                    ?: 0) + (lesson.displayStartTime?.minute ?: 0)
            val endMinute = startMinute + 45
            eventTimeRanges.add(DayView.EventTimeRange(startMinute, endMinute))
        }

        dayView.setEventViews(eventViews, eventTimeRanges)
        val firstEventTop = (firstEventMinute - dayView.startHour * 60) * dayView.minuteHeight
        dayScroll.scrollTo(0, firstEventTop.toInt())
    }

    override fun onResume() {
        super.onResume()
        if (dayScrollDelegate.isInitialized()) {
            val firstEventTop = (firstEventMinute - dayView.startHour * 60) * dayView.minuteHeight
            dayScroll.scrollTo(0, firstEventTop.toInt())
        }
    }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-24.
 */

package pl.szczodrzynski.edziennik.ui.home.cards

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.plusAssign
import androidx.core.view.setMargins
import androidx.lifecycle.Observer
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.sizeDp
import eu.szkolny.font.SzkolnyFont
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskAllFinishedEvent
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.db.entity.Profile
import pl.szczodrzynski.edziennik.data.db.enums.FeatureType
import pl.szczodrzynski.edziennik.data.db.full.LessonFull
import pl.szczodrzynski.edziennik.databinding.CardHomeTimetableBinding
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.ui.base.enums.NavTarget
import pl.szczodrzynski.edziennik.ui.dialogs.BellSyncTimeChooseDialog
import pl.szczodrzynski.edziennik.ui.home.CounterActivity
import pl.szczodrzynski.edziennik.ui.home.HomeCard
import pl.szczodrzynski.edziennik.ui.home.HomeCardAdapter
import pl.szczodrzynski.edziennik.ui.home.HomeFragment
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import pl.szczodrzynski.edziennik.utils.models.Week
import pl.szczodrzynski.navlib.colorAttr
import kotlin.coroutines.CoroutineContext

class HomeTimetableCard(
        override val id: Int,
        val app: App,
        val activity: MainActivity,
        val fragment: HomeFragment,
        val profile: Profile
) : HomeCard, CoroutineScope {
    companion object {
        private const val TAG = "HomeTimetableCard"
    }

    private var job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private lateinit var b: CardHomeTimetableBinding

    private val today = Date.getToday()
    private var timetableDate: Date = Date.getToday()
    private val searchEnd = today.clone().stepForward(0, 0, 7)

    private var allLessons = listOf<LessonFull>()
    private var lessons = listOf<LessonFull>()
    private var events = listOf<Event>()

    private var bellSyncDiffMillis = 0L
    private val syncedNow: Time
        get() = Time.fromMillis(Time.getNow().inMillis - bellSyncDiffMillis)

    private var counterJob: Job? = null
    private var counterStart: Time? = null
    private var counterEnd: Time? = null
    private var showAllLessons: Boolean = false
    private var subjectSpannable: CharSequence? = null

    private val ignoreCancelled = false

    private val countInSeconds: Boolean
        get() = app.config.timetable.countInSeconds

    override fun bind(position: Int, holder: HomeCardAdapter.ViewHolder) {
        holder.root.removeAllViews()
        b = CardHomeTimetableBinding.inflate(LayoutInflater.from(holder.root.context))
        b.root.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(8.dp)
        }
        holder.root += b.root

        b.settings.setImageDrawable(
            IconicsDrawable(activity, CommunityMaterial.Icon.cmd_cog_outline).apply {
                colorAttr(activity, R.attr.colorOnPrimaryContainer)
                sizeDp = 24
            }
        )

        b.bellSync.icon = IconicsDrawable(activity, SzkolnyFont.Icon.szf_alarm_bell_outline).apply {
            sizeDp = 24
        }

        b.showCounter.icon = IconicsDrawable(activity, CommunityMaterial.Icon2.cmd_fullscreen).apply {
            sizeDp = 24
        }

        b.bellSync.setOnClickListener {
            BellSyncTimeChooseDialog(
                    activity
            ).show()
        }

        b.showCounter.setOnClickListener {
            activity.startActivity(Intent(activity, CounterActivity::class.java))
        }

        b.root.onClick {
            activity.navigate(navTarget = NavTarget.TIMETABLE, args = Bundle(
                "timetableDate" to timetableDate.stringY_m_d,
            ))
        }

        if (app.profile.getStudentData("timetableNotPublic", false)) {
            b.timetableLayout.visibility = View.GONE
            b.notPublicLayout.visibility = View.VISIBLE
            return
        }

        // get current bell-sync params
        app.config.timetable.bellSyncDiff?.let {
            bellSyncDiffMillis = (it.hour * 60 * 60 * 1000 + it.minute * 60 * 1000 + it.second * 1000).toLong()
            bellSyncDiffMillis *= app.config.timetable.bellSyncMultiplier.toLong()
        }

        // get all lessons within the search bounds
        app.db.timetableDao().getBetweenDates(today, searchEnd).observe(fragment, Observer {
            allLessons = it
            update()
        })

        EventBus.getDefault().register(this)
    }

    private fun update() { launch {
        var checkedDays = 0
        val deferred = async(Dispatchers.Default) {
            // get the current bell-synced time
            val now = syncedNow

            // search for lessons to display
            val timetableDate = Date.getToday()
            lessons = allLessons.filter {
                it.profileId == profile.id
                        && it.displayDate == timetableDate
                        && it.displayEndTime > now
                        && !(it.isCancelled && ignoreCancelled)
            }
            if (!ignoreCancelled && lessons.all { it.isCancelled })
                // skip current day if all lessons are cancelled
                lessons = listOf()
            while ((lessons.isEmpty() || lessons.none {
                        it.type != Lesson.TYPE_NO_LESSONS
                                && (it.displayDate != today
                                || (it.displayDate == today
                                && it.displayEndTime != null
                                && it.displayEndTime!! >= now))
                                && !it.isCancelled
                    }) && checkedDays < 7) {

                timetableDate.stepForward(0, 0, 1)
                lessons = allLessons.filter {
                    it.profileId == profile.id
                            && it.displayDate == timetableDate
                }
                lessons = lessons.dropWhile { it.isCancelled }

                if (lessons.isEmpty())
                    break

                /*lessons = lessons.filterNot {
                    it.isCancelled && ignoreCancelled
                }*/

                checkedDays++
            }
            timetableDate
        }

        timetableDate = deferred.await()

        if (lessons.isEmpty() && checkedDays < 7) {
            // timetable is not downloaded yet
            b.timetableLayout.visibility = View.GONE
            b.noTimetableLayout.visibility = View.VISIBLE
            b.noLessonsLayout.visibility = View.GONE
            val weekStart = timetableDate.weekStart
            b.noTimetableText.setText(
                    R.string.home_timetable_no_timetable_text,
                    weekStart.stringY_m_d
            )
            b.noTimetableSync.onClick {
                it.isEnabled = false
                EdziennikTask.syncProfile(
                        profileId = profile.id,
                        featureTypes = setOf(FeatureType.TIMETABLE),
                        arguments = JsonObject(
                                "weekStart" to weekStart.stringY_m_d
                        )
                ).enqueue(activity)
            }

            timetableDate = Date.getToday()
            return@launch
        }
        if (lessons.none { !it.isCancelled } || lessons.size == 1 && lessons[0].type == Lesson.TYPE_NO_LESSONS) {
            // in next 7 days only NO_LESSONS is found
            b.timetableLayout.visibility = View.GONE
            b.noTimetableLayout.visibility = View.GONE
            b.noLessonsLayout.visibility = View.VISIBLE
            timetableDate = timetableDate.weekStart

            timetableDate = Date.getToday()
            return@launch
        }

        lessons = lessons.filter { it.type != Lesson.TYPE_NO_LESSONS }
        lessons.onEach { it.filterNotes() }

        b.timetableLayout.visibility = View.VISIBLE
        b.noTimetableLayout.visibility = View.GONE
        b.noLessonsLayout.visibility = View.GONE

        val isToday = today == timetableDate

        b.progress.visibility = View.GONE
        b.counter.visibility = View.GONE

        val now = syncedNow
        val firstLesson = lessons.firstOrNull { !it.isCancelled }
        val lastLesson = lessons.lastOrNull { !it.isCancelled }
        val skipFirst = if (firstLesson == null) 0 else lessons.indexOf(firstLesson)
        val skipLast = if (lastLesson == null) 0 else lessons.size - 1 - lessons.indexOf(lastLesson)
        val lessonCount = lessons.size - skipFirst - skipLast

        if (isToday) {
            // today
            b.dayInfo.setText(R.string.home_timetable_today)
            b.lessonInfo.setText(
                    R.string.home_timetable_lessons_remaining,
                    lessonCount,
                    lastLesson?.displayEndTime?.stringHM ?: "?"
            )
            counterStart = firstLesson?.displayStartTime
            counterEnd = firstLesson?.displayEndTime
            val isOngoing = counterStart <= now && now <= counterEnd
            val lessonRes = if (isOngoing)
                R.string.home_timetable_lesson_ongoing
            else
                R.string.home_timetable_lesson_not_started
            b.lessonBig.setText(lessonRes, firstLesson.subjectSpannable)
            firstLesson?.displayClassroom?.let {
                b.classroomHeading.visibility = View.VISIBLE
                b.classroom.visibility = View.VISIBLE
                b.classroom.text = it
            } ?: run {
                b.classroomHeading.visibility = View.GONE
                b.classroom.visibility = View.GONE
            }

            subjectSpannable = firstLesson.subjectSpannable

            counterJob = startCoroutineTimer(repeatMillis = 500) {
                count()
            }

            showAllLessons = !isOngoing
        }
        else {
            val isTomorrow = today.clone().stepForward(0, 0, 1) == timetableDate
            val dayInfoRes = if (isTomorrow) {
                // tomorrow
                R.string.home_timetable_tomorrow
            }
            else {
                val todayWeekStart = today.weekStart
                val dateWeekStart = timetableDate.weekStart
                if (todayWeekStart == dateWeekStart) {
                    // this week
                    R.string.home_timetable_date_this_week
                }
                else {
                    // future: not this week
                    R.string.home_timetable_date_future
                }
            }
            b.dayInfo.setText(dayInfoRes, Week.getFullDayName(timetableDate.weekDay), timetableDate.formattedString)
            b.lessonInfo.setText(
                    R.string.home_timetable_lessons_info,
                    lessonCount,
                    firstLesson?.displayStartTime?.stringHM ?: "?",
                    lastLesson?.displayEndTime?.stringHM ?: "?"
            )

            b.lessonBig.setText(R.string.home_timetable_lesson_first, firstLesson.subjectSpannable)
            b.counter.visibility = View.VISIBLE
            b.counter.text = firstLesson?.displayStartTime?.stringHM
            firstLesson?.displayClassroom?.let {
                b.classroomHeading.visibility = View.VISIBLE
                b.classroom.visibility = View.VISIBLE
                b.classroom.text = it
            } ?: run {
                b.classroomHeading.visibility = View.GONE
                b.classroom.visibility = View.GONE
            }

            showAllLessons = true
        }

        val text = mutableListOf<CharSequence>(
            if (showAllLessons)
                activity.getString(R.string.home_timetable_all_lessons)
            else
                activity.getString(R.string.home_timetable_later)
        )

        val nextLessons = if (showAllLessons)
            lessons.drop(skipFirst)
        else
            lessons.drop(skipFirst + 1)

        for (lesson in nextLessons) {
            text += listOf(
                    adjustTimeWidth(lesson.displayStartTime?.stringHM),
                    lesson.subjectSpannable
            ).concat(" ")
        }
        if (text.size == 1)
            text += activity.getString(R.string.home_timetable_later_no_lessons)
        b.nextLessons.text = text.concat("\n")
    }}

    private fun adjustTimeWidth(time: String?) = when {
        time == null -> ""
        time.length == 4 -> " $time  "
        else -> "$time "
    }

    private val LessonFull?.subjectSpannable: CharSequence
        get() = if (this == null) "?" else when {
            hasReplacingNotes() -> getNoteSubstituteText(showNotes = true) ?: "?"
            isCancelled -> displaySubjectName?.asStrikethroughSpannable() ?: "?"
            isChange -> displaySubjectName?.asItalicSpannable() ?: "?"
            else -> displaySubjectName ?: "?"
        }

    private fun count() {
        val counterStart = counterStart
        val counterEnd = counterEnd
        if (counterStart == null || counterEnd == null) {
            // there is no lesson to count
            b.progress.visibility = View.GONE
            b.counter.visibility = View.GONE
            this.counterJob?.cancel()
            return
        }

        val now = syncedNow
        if (now >= counterStart && showAllLessons) {
            // update "next lessons" view to remove current lesson
            this.counterJob?.cancel()
            this.counterStart = null
            this.counterEnd = null
            update()
            return
        }
        if (now > counterEnd) {
            // the lesson is already over
            b.progress.visibility = View.GONE
            b.counter.visibility = View.GONE
            this.counterJob?.cancel()
            this.counterStart = null
            this.counterEnd = null
            update() // check for new lessons to display
            return
        }

        val isOngoing = counterStart <= now && now <= counterEnd
        val lessonRes = if (isOngoing)
            R.string.home_timetable_lesson_ongoing
        else
            R.string.home_timetable_lesson_not_started
        b.lessonBig.setText(lessonRes, subjectSpannable ?: "")

        if (now < counterStart) {
            // the lesson hasn't yet started
            b.progress.visibility = View.GONE
            b.counter.visibility = View.VISIBLE
            val diff = counterStart - now
            if (diff >= 60 * MINUTE)
                b.counter.text = counterStart.stringHM
            else
                b.counter.text = activity.timeTill(diff.toInt(), " ", countInSeconds)
        }
        else {
            // the lesson is right now
            b.progress.visibility = View.VISIBLE
            b.counter.visibility = View.VISIBLE
            val lessonLength = counterEnd - counterStart
            val timePassed = now - counterStart
            val timeLeft = counterEnd - now
            b.counter.text = activity.timeLeft(timeLeft.toInt(), " ", countInSeconds)
            b.progress.max = lessonLength.toInt()
            b.progress.progress = timePassed.toInt()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSyncFinishedEvent(event: ApiTaskAllFinishedEvent) {
        b.noTimetableSync.isEnabled = true
    }

    override fun unbind(position: Int, holder: HomeCardAdapter.ViewHolder) = Unit
}

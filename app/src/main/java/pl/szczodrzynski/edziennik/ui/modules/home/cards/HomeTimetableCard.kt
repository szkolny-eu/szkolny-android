/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-24.
 */

package pl.szczodrzynski.edziennik.ui.modules.home.cards

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
import com.mikepenz.iconics.typeface.library.szkolny.font.SzkolnyFont
import com.mikepenz.iconics.utils.sizeDp
import kotlinx.coroutines.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.LOGIN_TYPE_LIBRUS
import pl.szczodrzynski.edziennik.data.api.events.ApiTaskAllFinishedEvent
import pl.szczodrzynski.edziennik.data.api.task.EdziennikTask
import pl.szczodrzynski.edziennik.data.db.modules.events.Event
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.data.db.modules.timetable.Lesson
import pl.szczodrzynski.edziennik.data.db.modules.timetable.LessonFull
import pl.szczodrzynski.edziennik.databinding.CardHomeTimetableBinding
import pl.szczodrzynski.edziennik.ui.dialogs.bell.BellSyncTimeChooseDialog
import pl.szczodrzynski.edziennik.ui.modules.home.HomeCard
import pl.szczodrzynski.edziennik.ui.modules.home.HomeCardAdapter
import pl.szczodrzynski.edziennik.ui.modules.home.HomeFragment
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
        get() = Time.fromMillis(Time.getNow().inMillis + bellSyncDiffMillis)

    private var counterJob: Job? = null
    private var counterStart: Time? = null
    private var counterEnd: Time? = null
    private var subjectSpannable: CharSequence? = null

    private val ignoreCancelled = true

    override fun bind(position: Int, holder: HomeCardAdapter.ViewHolder) {
        holder.root.removeAllViews()
        b = CardHomeTimetableBinding.inflate(LayoutInflater.from(holder.root.context))
        b.root.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(8.dp)
        }
        holder.root += b.root

        b.settings.setImageDrawable(IconicsDrawable(activity, CommunityMaterial.Icon2.cmd_settings_outline)
                .colorAttr(activity, R.attr.colorIcon)
                .sizeDp(20))

        b.bellSync.setImageDrawable(IconicsDrawable(activity, SzkolnyFont.Icon.szf_alarm_bell_outline)
                .colorAttr(activity, R.attr.colorIcon)
                .sizeDp(20))

        b.bellSync.setOnClickListener {
            BellSyncTimeChooseDialog(
                    activity
            )
        }

        b.root.onClick {
            activity.loadTarget(MainActivity.DRAWER_ITEM_TIMETABLE, Bundle().apply {
                putString("timetableDate", timetableDate.stringY_m_d)
            })
        }

        if (app.profile.loginStoreType == LOGIN_TYPE_LIBRUS && app.profile.getLoginData("timetableNotPublic", false)) {
            b.timetableLayout.visibility = View.GONE
            b.notPublicLayout.visibility = View.VISIBLE
            return
        }

        // get current bell-sync params
        app.config.timetable.bellSyncDiff?.let {
            bellSyncDiffMillis = (it.hour * 60 * 60 * 1000 + it.minute * 60 * 1000 + it.second * 1000).toLong()
            bellSyncDiffMillis *= app.config.timetable.bellSyncMultiplier.toLong()
            bellSyncDiffMillis *= -1
        }

        // get all lessons within the search bounds
        app.db.timetableDao().getBetweenDates(today, searchEnd).observe(fragment, Observer {
            allLessons = it
            update()
        })

        EventBus.getDefault().register(this)
    }

    private fun update() { launch {
        val deferred = async(Dispatchers.Default) {
            // get the current bell-synced time
            val now = syncedNow

            // search for lessons to display
            val timetableDate = Date.getToday()
            var checkedDays = 0
            lessons = allLessons.filter {
                it.profileId == profile.id
                        && it.displayDate == timetableDate
                        && it.displayEndTime > now
                        && !(it.isCancelled && ignoreCancelled)
            }
            while ((lessons.isEmpty() || lessons.none {
                        it.type != Lesson.TYPE_NO_LESSONS
                                && (it.displayDate != today
                                || (it.displayDate == today
                                && it.displayEndTime != null
                                && it.displayEndTime!! >= now))
                    }) && checkedDays < 7) {

                timetableDate.stepForward(0, 0, 1)
                lessons = allLessons.filter {
                    it.profileId == profile.id
                            && it.displayDate == timetableDate
                            && !(it.isCancelled && ignoreCancelled)
                }

                if (lessons.isEmpty() && timetableDate.weekDay <= 5)
                    break

                checkedDays++
            }
            timetableDate
        }

        timetableDate = deferred.await()

        if (lessons.isEmpty()) {
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
                        profileId = App.profileId,
                        viewIds = listOf(
                                MainActivity.DRAWER_ITEM_TIMETABLE to 0
                        ),
                        arguments = JsonObject(
                                "weekStart" to weekStart
                        )
                ).enqueue(activity)
            }
            return@launch
        }
        if (lessons.size == 1 && lessons[0].type == Lesson.TYPE_NO_LESSONS) {
            // in next 7 days only NO_LESSONS is found
            b.timetableLayout.visibility = View.GONE
            b.noTimetableLayout.visibility = View.GONE
            b.noLessonsLayout.visibility = View.VISIBLE
            timetableDate = timetableDate.weekStart
            return@launch
        }

        b.timetableLayout.visibility = View.VISIBLE
        b.noTimetableLayout.visibility = View.GONE
        b.noLessonsLayout.visibility = View.GONE

        val isToday = today == timetableDate

        b.progress.visibility = View.GONE
        b.counter.visibility = View.GONE

        val now = syncedNow
        val firstLesson = lessons.firstOrNull()
        val lastLesson = lessons.lastOrNull()

        if (isToday) {
            // today
            b.dayInfo.setText(R.string.home_timetable_today)
            b.lessonInfo.setText(
                    R.string.home_timetable_lessons_remaining,
                    lessons.size,
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
                b.classroom.visibility = View.VISIBLE
                b.classroom.text = it
            } ?: run {
                b.classroom.visibility = View.GONE
            }

            subjectSpannable = firstLesson.subjectSpannable

            counterJob = startCoroutineTimer(repeatMillis = 1000) {
                count()
            }
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
                    lessons.size,
                    firstLesson?.displayStartTime?.stringHM ?: "?",
                    lastLesson?.displayEndTime?.stringHM ?: "?"
            )

            b.lessonBig.setText(R.string.home_timetable_lesson_first, firstLesson.subjectSpannable)
            firstLesson?.displayClassroom?.let {
                b.classroom.visibility = View.VISIBLE
                b.classroom.text = it
            } ?: run {
                b.classroom.visibility = View.GONE
            }
        }

        val text = mutableListOf<CharSequence>(
                activity.getString(R.string.home_timetable_later)
        )
        var first = true
        for (lesson in lessons) {
            if (first) { first = false; continue }
            text += listOf(
                    lesson.displayStartTime?.stringHM,
                    lesson.subjectSpannable
            ).concat(" ")
        }
        if (text.size == 1)
            text += activity.getString(R.string.home_timetable_later_no_lessons)
        b.nextLessons.text = text.concat("\n")
    }}

    private val LessonFull?.subjectSpannable: CharSequence
        get() = if (this == null) "?" else when {
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
            b.counter.text = activity.timeTill(diff.toInt(), "\n")
        }
        else {
            // the lesson is right now
            b.progress.visibility = View.VISIBLE
            b.counter.visibility = View.VISIBLE
            val lessonLength = counterEnd - counterStart
            val timePassed = now - counterStart
            val timeLeft = counterEnd - now
            b.counter.text = activity.timeLeft(timeLeft.toInt(), "\n")
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

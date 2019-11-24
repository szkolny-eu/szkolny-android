/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-24.
 */

package pl.szczodrzynski.edziennik.ui.modules.home.cards

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.plusAssign
import androidx.core.view.setMargins
import androidx.lifecycle.Observer
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.modules.events.Event
import pl.szczodrzynski.edziennik.data.db.modules.profiles.Profile
import pl.szczodrzynski.edziennik.data.db.modules.timetable.Lesson
import pl.szczodrzynski.edziennik.data.db.modules.timetable.LessonFull
import pl.szczodrzynski.edziennik.databinding.CardHomeTimetableBinding
import pl.szczodrzynski.edziennik.ui.modules.home.HomeCard
import pl.szczodrzynski.edziennik.ui.modules.home.HomeCardAdapter
import pl.szczodrzynski.edziennik.ui.modules.home.HomeFragmentV2
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import kotlin.coroutines.CoroutineContext

class HomeTimetableCard(
        val id: Int,
        val app: App,
        val activity: MainActivity,
        val fragment: HomeFragmentV2,
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
    private val searchEnd = today.clone().stepForward(0, 0, 7)

    private var allLessons = listOf<LessonFull>()
    private var lessons = listOf<LessonFull>()
    private var events = listOf<Event>()

    override fun bind(position: Int, holder: HomeCardAdapter.ViewHolder) {
        holder.root.removeAllViews()
        b = CardHomeTimetableBinding.inflate(LayoutInflater.from(holder.root.context))
        b.root.layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            setMargins(8.dp)
        }
        holder.root += b.root

        // get all lessons within the search bounds
        app.db.timetableDao().getBetweenDates(today, searchEnd).observe(fragment, Observer {
            allLessons = it
            update()
        })
    }

    private fun update() { launch {
        val deferred = async(Dispatchers.Default) {
            // get current bell-sync params
            var bellSyncDiffMillis: Long = 0
            if (app.appConfig.bellSyncDiff != null) {
                bellSyncDiffMillis = (app.appConfig.bellSyncDiff.hour * 60 * 60 * 1000 + app.appConfig.bellSyncDiff.minute * 60 * 1000 + app.appConfig.bellSyncDiff.second * 1000).toLong()
                bellSyncDiffMillis *= app.appConfig.bellSyncMultiplier.toLong()
                bellSyncDiffMillis *= -1
            }
            // get the current bell-synced time
            val now = Time.fromMillis(Time.getNow().inMillis + bellSyncDiffMillis)

            // search for lessons to display
            val timetableDate = Date.getToday()
            var checkedDays = 0
            lessons = allLessons.filter { it.profileId == profile.id && it.displayDate == timetableDate && it.displayEndTime > now && it.type != Lesson.TYPE_NO_LESSONS }
            while ((lessons.isEmpty() || lessons.none {
                        it.displayDate != today || (it.displayDate == today && it.displayEndTime != null && it.displayEndTime!! >= now)
                    }) && checkedDays < 7) {
                timetableDate.stepForward(0, 0, 1)
                lessons = allLessons.filter { it.profileId == profile.id && it.displayDate == timetableDate && it.type != Lesson.TYPE_NO_LESSONS }
                checkedDays++
            }
        }
        deferred.await()

        val text = StringBuilder()
        for (lesson in lessons) {
            text += lesson.displayStartTime?.stringHM+" "+lesson.displaySubjectName+"\n"
        }
        b.text.text = text.toString()
    }}

    override fun unbind(position: Int, holder: HomeCardAdapter.ViewHolder) = Unit
}
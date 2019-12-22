/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-21
 */

package pl.szczodrzynski.edziennik.ui.modules.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.modules.timetable.Lesson
import pl.szczodrzynski.edziennik.data.db.modules.timetable.LessonFull
import pl.szczodrzynski.edziennik.databinding.ActivityCounterBinding
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import kotlin.coroutines.CoroutineContext

class CounterActivity : AppCompatActivity(), CoroutineScope {

    private var job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private var counterJob: Job? = null

    private val app by lazy { application as App }
    private lateinit var b: ActivityCounterBinding

    private val lessonList = mutableListOf<LessonFull>()

    private var bellSyncDiffMillis = 0L
    private val syncedNow: Time
        get() = Time.fromMillis(Time.getNow().inMillis - bellSyncDiffMillis)

    private val countInSeconts: Boolean
        get() = app.config.timetable.countInSeconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityCounterBinding.inflate(layoutInflater)
        setContentView(b.root)
        initView()
    }

    private fun initView() { launch {
        withContext(Dispatchers.Default) {
            lessonList.apply {
                clear()
                addAll(app.db.timetableDao().getForDateNow(App.profileId, Date.getToday())
                        .filter {
                            it.type != Lesson.TYPE_NO_LESSONS && it.type != Lesson.TYPE_CANCELLED &&
                                    it.type != Lesson.TYPE_SHIFTED_SOURCE
                        })
            }
        }

        app.config.timetable.bellSyncDiff?.let {
            bellSyncDiffMillis = (it.hour * 60 * 60 * 1000 + it.minute * 60 * 1000 + it.second * 1000).toLong()
            bellSyncDiffMillis *= app.config.timetable.bellSyncMultiplier.toLong()
        }

        counterJob = startCoroutineTimer(repeatMillis = 1000) {
            update()
        }
    }}

    private fun update() {
        if (lessonList.isEmpty()) {
            b.lessonName.text = app.getString(R.string.no_lessons_today)
            b.timeLeft.text = ""
        } else {
            val now = syncedNow

            val next = lessonList.firstOrNull {
                 it.displayStartTime != null && it.displayStartTime!! > now
            }

            val actual = lessonList.firstOrNull {
                it.displayStartTime != null && it.displayEndTime != null &&
                        it.displayStartTime!! <= now && now <= it.displayEndTime!!
            }

            when {
                actual != null -> {
                    b.lessonName.text = actual.displaySubjectName

                    val left = actual.displayEndTime!! - now
                    b.timeLeft.text = timeLeft(left.toInt(), "\n", countInSeconts)
                }
                next != null -> {
                    b.lessonName.text = next.displaySubjectName

                    val till = next.displayStartTime!! - now
                    b.timeLeft.text = timeTill(till.toInt(), "\n", countInSeconts)
                }
                else -> {
                    b.lessonName.text = app.getString(R.string.lessons_finished)
                    b.timeLeft.text = ""
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        counterJob?.cancel()
    }
}

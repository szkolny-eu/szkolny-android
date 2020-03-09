/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-21
 */

package pl.szczodrzynski.edziennik.ui.modules.home

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.szkolny.font.SzkolnyFont
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.iconics.utils.sizeDp
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.db.full.LessonFull
import pl.szczodrzynski.edziennik.databinding.ActivityCounterBinding
import pl.szczodrzynski.edziennik.ui.dialogs.bell.BellSyncTimeChooseDialog
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

    private val countInSeconds: Boolean
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

        b.bellSync.setImageDrawable(IconicsDrawable(this@CounterActivity, SzkolnyFont.Icon.szf_alarm_bell_outline)
                .colorInt(0xff404040.toInt())
                .sizeDp(36))
        b.bellSync.onClick {
            BellSyncTimeChooseDialog(activity = this@CounterActivity)
        }

        app.config.timetable.bellSyncDiff?.let {
            bellSyncDiffMillis = (it.hour * 60 * 60 * 1000 + it.minute * 60 * 1000 + it.second * 1000).toLong()
            bellSyncDiffMillis *= app.config.timetable.bellSyncMultiplier.toLong()
        }

        counterJob = startCoroutineTimer(repeatMillis = 500) {
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
                    b.timeLeft.text = timeLeft(left.toInt(), "\n", countInSeconds)
                }
                next != null -> {
                    b.lessonName.text = next.displaySubjectName

                    val till = next.displayStartTime!! - now
                    b.timeLeft.text = timeTill(till.toInt(), "\n", countInSeconds)
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

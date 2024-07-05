/*
 * Copyright (c) Kacper Ziubryniewicz 2019-12-21
 */

package pl.szczodrzynski.edziennik.ui.home

import android.graphics.BitmapFactory
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jetradarmobile.snowfall.SnowfallView
import eu.szkolny.font.SzkolnyFont
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Lesson
import pl.szczodrzynski.edziennik.data.db.full.LessonFull
import pl.szczodrzynski.edziennik.databinding.ActivityCounterBinding
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ext.startCoroutineTimer
import pl.szczodrzynski.edziennik.ext.timeLeft
import pl.szczodrzynski.edziennik.ext.timeTill
import pl.szczodrzynski.edziennik.ext.toDrawable
import pl.szczodrzynski.edziennik.ui.dialogs.BellSyncTimeChooseDialog
import pl.szczodrzynski.edziennik.utils.BigNightUtil
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
                addAll(app.db.timetableDao().getAllForDateNow(App.profileId, Date.getToday())
                        .filter {
                            it.type != Lesson.TYPE_NO_LESSONS && it.type != Lesson.TYPE_CANCELLED &&
                                    it.type != Lesson.TYPE_SHIFTED_SOURCE
                        })
            }
            lessonList.onEach { it.filterNotes() }
        }

        b.bellSync.setImageDrawable(
            SzkolnyFont.Icon.szf_alarm_bell_outline
                .toDrawable(0xff404040.toInt(), sizeDp = 36)
        )
        b.bellSync.onClick {
            BellSyncTimeChooseDialog(activity = this@CounterActivity).show()
        }

        app.config.timetable.bellSyncDiff?.let {
            bellSyncDiffMillis = (it.hour * 60 * 60 * 1000 + it.minute * 60 * 1000 + it.second * 1000).toLong()
            bellSyncDiffMillis *= app.config.timetable.bellSyncMultiplier.toLong()
        }

        counterJob = startCoroutineTimer(repeatMillis = 500) {
            update()
        }

        // IT'S WINTER MY DUDES
        val today = Date.getToday()
        if ((today.month / 3 % 4 == 0) && app.config.ui.snowfall) {
            b.rootFrame.addView(layoutInflater.inflate(R.layout.snowfall, b.rootFrame, false))
        } else if (app.config.ui.eggfall && BigNightUtil().isDataWielkanocyNearDzisiaj()) {
            val eggfall = layoutInflater.inflate(
                R.layout.eggfall,
                b.rootFrame,
                false
            ) as SnowfallView
            eggfall.setSnowflakeBitmaps(listOf(
                BitmapFactory.decodeResource(resources, R.drawable.egg1),
                BitmapFactory.decodeResource(resources, R.drawable.egg2),
                BitmapFactory.decodeResource(resources, R.drawable.egg3),
                BitmapFactory.decodeResource(resources, R.drawable.egg4),
                BitmapFactory.decodeResource(resources, R.drawable.egg5),
                BitmapFactory.decodeResource(resources, R.drawable.egg6)
            ))
            b.rootFrame.addView(eggfall)
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
                    b.lessonName.text = actual.getNoteSubstituteText(showNotes = true)
                        ?: actual.displaySubjectName

                    val left = actual.displayEndTime!! - now
                    b.timeLeft.text = timeLeft(left.toInt(), "\n", countInSeconds)
                }
                next != null -> {
                    b.lessonName.text = next.getNoteSubstituteText(showNotes = true)
                        ?: next.displaySubjectName

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

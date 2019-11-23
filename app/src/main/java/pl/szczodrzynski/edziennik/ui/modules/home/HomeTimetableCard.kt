/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-22
 */

package pl.szczodrzynski.edziennik.ui.modules.home

import android.content.Intent
import android.os.AsyncTask
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.databinding.DataBindingUtil
import com.afollestad.materialdialogs.DialogAction
import com.afollestad.materialdialogs.MaterialDialog
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.modules.events.Event
import pl.szczodrzynski.edziennik.data.db.modules.timetable.Lesson
import pl.szczodrzynski.edziennik.databinding.CardTimetableBinding
import pl.szczodrzynski.edziennik.ui.modules.home.HomeFragment.updateInterval
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Time
import java.util.*

class HomeTimetableCard(
        private val app: App,
        private val activity: MainActivity,
        private val homeFragment: HomeFragment,
        private val layoutInflater: LayoutInflater,
        private val insertPoint: ViewGroup
) {

    private lateinit var timetableTimer: Timer
    private lateinit var b: CardTimetableBinding

    private var bellSyncTime: Time? = null

    private companion object {
        const val TIME_TILL = 0
        const val TIME_LEFT = 1
    }

    private var counterType = TIME_TILL
    private val counterTarget = Time(0, 0, 0)

    private val lessons = mutableListOf<Lesson>()
    private val events = mutableListOf<Event>()

    fun run() {
        timetableTimer = Timer()
        b = DataBindingUtil.inflate(layoutInflater, R.layout.card_timetable, null, false)

        update()

        insertPoint.addView(b.root, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))

        b.cardTimetableFullscreenCounter.setOnClickListener {
            activity.startActivity(Intent(activity, CounterActivity::class.java))
        }

        b.cardTimetableBellSync.setOnClickListener {
            if (bellSyncTime == null) {
                MaterialDialog.Builder(activity)
                        .title(R.string.bell_sync_title)
                        .content(R.string.bell_sync_cannot_now)
                        .positiveText(R.string.ok)
                        .show()
            } else {
                MaterialDialog.Builder(activity)
                        .title(R.string.bell_sync_title)
                        .content(app.getString(R.string.bell_sync_howto, bellSyncTime!!.stringHM).toString() +
                                when {
                                    app.appConfig.bellSyncDiff != null -> app.getString(R.string.bell_sync_current_dialog,
                                            (if (app.appConfig.bellSyncMultiplier == -1) "-" else "+") + app.appConfig.bellSyncDiff.stringHMS)
                                    else -> ""
                                })
                        .positiveText(R.string.ok)
                        .negativeText(R.string.cancel)
                        .neutralText(R.string.reset)
                        .onPositive { _, _: DialogAction? ->
                            val bellDiff = Time.diff(Time.getNow(), bellSyncTime)
                            app.appConfig.bellSyncDiff = bellDiff
                            app.appConfig.bellSyncMultiplier = if (bellSyncTime!!.value > Time.getNow().value) -1 else 1
                            app.saveConfig("bellSyncDiff", "bellSyncMultiplier")

                            MaterialDialog.Builder(activity)
                                    .title(R.string.bell_sync_title)
                                    .content(app.getString(R.string.bell_sync_results, if (bellSyncTime!!.value > Time.getNow().value) "-" else "+", bellDiff.stringHMS))
                                    .positiveText(R.string.ok)
                                    .show()
                        }
                        .onNeutral { _, _ ->
                            MaterialDialog.Builder(activity)
                                    .title(R.string.bell_sync_title)
                                    .content(R.string.bell_sync_reset_confirm)
                                    .positiveText(R.string.yes)
                                    .negativeText(R.string.no)
                                    .onPositive { _, _ ->
                                        app.appConfig.bellSyncDiff = null
                                        app.appConfig.bellSyncMultiplier = 0
                                        app.saveConfig("bellSyncDiff", "bellSyncMultiplier")
                                    }
                                    .show()
                        }
                        .show()
            }
        }

        HomeFragment.buttonAddDrawable(activity, b.cardTimetableButton, CommunityMaterial.Icon.cmd_arrow_right)
    }

    fun destroy() {
        try {
            timetableTimer.apply {
                cancel()
                purge()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun update() {
        if (!homeFragment.isAdded) return

        val now = Time.getNow()

        val syncedNow: Time = when (app.appConfig.bellSyncDiff != null) {
            true -> when {
                app.appConfig.bellSyncMultiplier < 0 -> Time.sum(now, app.appConfig.bellSyncDiff)
                app.appConfig.bellSyncMultiplier > 0 -> Time.diff(now, app.appConfig.bellSyncDiff)
                else -> now
            }
            else -> now
        }

        if (lessons.size == 0 || syncedNow.value > counterTarget.value) {
            findLessons(syncedNow)
        } else {
            scheduleUpdate(updateCounter(syncedNow))
        }
    }

    private fun updateCounter(syncedNow: Time): Long {
        val diff = Time.diff(counterTarget, syncedNow)
        b.cardTimetableTimeLeft.text = when (counterType) {
            TIME_TILL -> HomeFragment.timeTill(app, diff, app.appConfig.countInSeconds)
            else -> HomeFragment.timeLeft(app, diff, app.appConfig.countInSeconds)
        }
        bellSyncTime = counterTarget.clone()
        b.cardTimetableFullscreenCounter.visibility = View.VISIBLE
        return updateInterval(app, diff)
    }

    private fun scheduleUpdate(newRefreshInterval: Long) {
        timetableTimer.schedule(object : TimerTask() {
            override fun run() {
                activity.runOnUiThread { update() }
            }
        }, newRefreshInterval)
    }

    private fun findLessons(syncedNow: Time) {
        AsyncTask.execute {
            val today = Date.getToday()
            val searchEnd = Date.getToday().stepForward(0, 0, -today.weekDay)

            lessons.apply {
                clear()
                addAll(app.db.timetableDao().getBetweenDatesNow(today, searchEnd))
            }
        }
    }
}

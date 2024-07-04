/*
 * Copyright (c) Kacper Ziubryniewicz 2020-1-6
 */

package pl.szczodrzynski.edziennik.ui.timetable

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.datepicker.MaterialDatePicker
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.szkolny.font.SzkolnyFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.enums.FeatureType
import pl.szczodrzynski.edziennik.data.enums.MetadataType
import pl.szczodrzynski.edziennik.databinding.FragmentTimetableV2Binding
import pl.szczodrzynski.edziennik.ext.Bundle
import pl.szczodrzynski.edziennik.ext.JsonObject
import pl.szczodrzynski.edziennik.ext.getSchoolYearConstrains
import pl.szczodrzynski.edziennik.ext.getStudentData
import pl.szczodrzynski.edziennik.ui.base.fragment.PagerFragment
import pl.szczodrzynski.edziennik.ui.dialogs.settings.TimetableConfigDialog
import pl.szczodrzynski.edziennik.ui.event.EventManualDialog
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Week
import pl.szczodrzynski.navlib.bottomsheet.items.BottomSheetPrimaryItem

class TimetableFragment : PagerFragment<FragmentTimetableV2Binding, MainActivity>(
    inflater = FragmentTimetableV2Binding::inflate,
) {
    companion object {
        private const val TAG = "TimetableFragment"
        const val ACTION_SCROLL_TO_DATE = "pl.szczodrzynski.edziennik.timetable.SCROLL_TO_DATE"
        const val ACTION_RELOAD_PAGES = "pl.szczodrzynski.edziennik.timetable.RELOAD_PAGES"
        const val DEFAULT_START_HOUR = 6
        const val DEFAULT_END_HOUR = 19
        var pageSelection: Date? = null
    }

    override fun getFab() = R.string.timetable_today to SzkolnyFont.Icon.szf_calendar_today_outline
    override fun getMarkAsReadType() = MetadataType.LESSON_CHANGE
    override fun getBottomSheetItems() = listOf(
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.menu_timetable_sync)
            .withIcon(CommunityMaterial.Icon.cmd_calendar_sync_outline)
            .withOnClickListener {
                activity.bottomSheet.close()
                val date = pageSelection ?: Date.getToday()
                val weekStart = date.weekStart.stringY_m_d
                EdziennikTask.syncProfile(
                    profileId = App.profileId,
                    featureTypes = setOf(FeatureType.TIMETABLE),
                    arguments = JsonObject(
                        "weekStart" to weekStart
                    )
                ).enqueue(activity)
            },
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.timetable_select_day)
            .withIcon(SzkolnyFont.Icon.szf_calendar_today_outline)
            .withOnClickListener { _ ->
                activity.bottomSheet.close()
                val date = pageSelection ?: Date.getToday()
                MaterialDatePicker.Builder.datePicker()
                    .setSelection(date.inMillisUtc)
                    .setCalendarConstraints(app.profile.getSchoolYearConstrains())
                    .build()
                    .apply {
                        addOnPositiveButtonClickListener { millis ->
                            val dateSelected = Date.fromMillisUtc(millis)
                            val index = items.indexOfFirst { it == dateSelected }
                            if (index != -1)
                                b.viewPager.setCurrentItem(index, true)
                        }
                    }
                    .show(activity.supportFragmentManager, TAG)
            },
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.menu_add_event)
            .withDescription(R.string.menu_add_event_desc)
            .withIcon(SzkolnyFont.Icon.szf_calendar_plus_outline)
            .withOnClickListener {
                activity.bottomSheet.close()
                EventManualDialog(
                    activity,
                    App.profileId,
                    defaultDate = items[savedPageSelection]
                ).show()
            },
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.menu_generate_block_timetable)
            .withDescription(R.string.menu_generate_block_timetable_desc)
            .withIcon(CommunityMaterial.Icon3.cmd_table_large)
            .withOnClickListener {
                activity.bottomSheet.close()
                GenerateBlockTimetableDialog(activity)
            },
        BottomSheetPrimaryItem(true)
            .withTitle(R.string.menu_timetable_config)
            .withIcon(CommunityMaterial.Icon.cmd_cog_outline)
            .withOnClickListener {
                activity.bottomSheet.close()
                TimetableConfigDialog(activity, false, null, null).show()
            }
    )

    override fun getTabLayout() = b.tabLayout
    override fun getViewPager() = b.viewPager

    override fun getPageCount() = items.size
    override fun getPageFragment(position: Int) = TimetableDayFragment().apply {
        arguments = Bundle(
            "date" to items[position].value,
            "startHour" to startHour,
            "endHour" to endHour,
        )
    }

    override fun getPageTitle(position: Int): String {
        val date = items[position]
        var pageTitle = Week.getFullDayName(date.weekDay)
        if (date > weekEnd || date < weekStart) {
            pageTitle += ", ${date.stringDm}"
        }
        return pageTitle
    }

    private var startHour = DEFAULT_START_HOUR
    private var endHour = DEFAULT_END_HOUR
    private val today by lazy { Date.getToday() }
    private val weekStart by lazy { today.weekStart }
    private val weekEnd by lazy { weekStart.clone().stepForward(0, 0, 6) }
    private val items = mutableListOf<Date>()
    private var fabShown = false

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, i: Intent) {
            if (!isAdded)
                return
            when (i.action) {
                ACTION_SCROLL_TO_DATE -> {
                    val dateStr = i.extras?.getString("timetableDate", null) ?: return
                    val date = Date.fromY_m_d(dateStr)
                    goToPage(items.indexOf(date))
                }

                ACTION_RELOAD_PAGES -> {
                    b.viewPager.adapter?.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ContextCompat.registerReceiver(
            activity,
            broadcastReceiver,
            IntentFilter(ACTION_SCROLL_TO_DATE),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        ContextCompat.registerReceiver(
            activity,
            broadcastReceiver,
            IntentFilter(ACTION_RELOAD_PAGES),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onPause() {
        super.onPause()
        activity.unregisterReceiver(broadcastReceiver)
    }

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        if (app.profile.getStudentData("timetableNotPublic", false)) {
            b.timetableLayout.visibility = View.GONE
            b.timetableNotPublicLayout.visibility = View.VISIBLE
            return
        }
        b.timetableLayout.visibility = View.VISIBLE
        b.timetableNotPublicLayout.visibility = View.GONE

        val deferred = async(Dispatchers.Default) {
            items.clear()

            val monthDayCount = listOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

            val yearStart = app.profile.dateSemester1Start.clone() ?: return@async
            val yearEnd = app.profile.dateYearEnd
            while (yearStart.value <= yearEnd.value) {
                items += yearStart.clone()
                var maxDays = monthDayCount[yearStart.month - 1]
                if (yearStart.month == 2 && yearStart.isLeap)
                    maxDays++
                yearStart.day++
                if (yearStart.day > maxDays) {
                    yearStart.day = 1
                    yearStart.month++
                }
                if (yearStart.month > 12) {
                    yearStart.month = 1
                    yearStart.year++
                }
            }

            val lessonRanges = app.db.lessonRangeDao().getAllNow(App.profileId)
            startHour = lessonRanges.minOfOrNull { it.startTime.hour } ?: DEFAULT_START_HOUR
            endHour = lessonRanges.maxOfOrNull { it.endTime.hour }?.plus(1) ?: DEFAULT_END_HOUR
        }
        deferred.await()
        if (!isAdded)
            return

        val selectedDate = arguments?.getString("timetableDate", "")
            ?.let { if (it.isBlank()) null else Date.fromY_m_d(it) }
        savedPageSelection = items.indexOfFirst { it == (selectedDate ?: today) }

        super.onViewReady(savedInstanceState)
    }

    override suspend fun onFabClick() {
        b.viewPager.setCurrentItem(items.indexOfFirst { it == today }, true)
    }

    override suspend fun onPageSelected(position: Int) {
        activity.navView.bottomBar.fabEnable = items[position] != today
        if (activity.navView.bottomBar.fabEnable && !fabShown) {
            activity.gainAttentionFAB()
            fabShown = true
        }
    }

    /*private fun markLessonsAsSeen() = pageSelection?.let { date ->
        app.db.timetableDao().getForDate(App.profileId, date).observeOnce(this@TimetableFragment, Observer { lessons ->
            lessons.forEach { lesson ->
                if (lesson.type != Lesson.TYPE_NORMAL && lesson.type != Lesson.TYPE_NO_LESSONS
                        && !lesson.seen) {
                    app.db.metadataDao().setSeen(lesson.profileId, lesson, true)
                }
            }
        })
    }*/
}

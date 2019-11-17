package pl.szczodrzynski.edziennik.ui.modules.timetable.v2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.api.v2.LOGIN_TYPE_LIBRUS
import pl.szczodrzynski.edziennik.databinding.FragmentTimetableV2Binding
import pl.szczodrzynski.edziennik.utils.Themes
import pl.szczodrzynski.edziennik.utils.models.Date
import kotlin.coroutines.CoroutineContext

class TimetableFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "TimetableFragment"
        const val ACTION_SCROLL_TO_DATE = "pl.szczodrzynski.edziennik.timetable.SCROLL_TO_DATE"
        const val DEFAULT_START_HOUR = 6
        const val DEFAULT_END_HOUR = 19
        var pageSelection: Date? = null
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: FragmentTimetableV2Binding

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private var fabShown = false
    private val items = mutableListOf<Date>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        if (context == null)
            return null
        app = activity.application as App
        job = Job()
        context!!.theme.applyStyle(Themes.appTheme, true)
        if (app.profile == null)
            return inflater.inflate(R.layout.fragment_loading, container, false)
        // activity, context and profile is valid
        b = FragmentTimetableV2Binding.inflate(inflater)
        b.refreshLayout.setParent(activity.swipeRefreshLayout)
        return b.root
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, i: Intent) {
            if (!isAdded)
                return
            val dateStr = i.extras?.getString("date", null) ?: return
            val date = Date.fromY_m_d(dateStr)
            b.viewPager.setCurrentItem(items.indexOf(date), true)
        }
    }
    override fun onResume() {
        super.onResume()
        activity.registerReceiver(broadcastReceiver, IntentFilter(ACTION_SCROLL_TO_DATE))
    }
    override fun onPause() {
        super.onPause()
        activity.unregisterReceiver(broadcastReceiver)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { launch {
        // TODO check if app, activity, b can be null
        if (app.profile == null || !isAdded)
            return@launch

        if (app.profile.loginStoreType == LOGIN_TYPE_LIBRUS && app.profile.getLoginData("timetableNotPublic", false)) {
            b.timetableLayout.visibility = View.GONE
            b.timetableNotPublicLayout.visibility = View.VISIBLE
            return@launch
        }
        b.timetableLayout.visibility = View.VISIBLE
        b.timetableNotPublicLayout.visibility = View.GONE

        val today = Date.getToday().value
        var startHour = DEFAULT_START_HOUR
        var endHour = DEFAULT_END_HOUR
        val deferred = async(Dispatchers.Default) {
            items.clear()

            val monthDayCount = listOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

            val yearStart = app.profile.dateSemester1Start?.clone() ?: return@async
            val yearEnd = app.profile.dateYearEnd ?: return@async
            while (yearStart.value <= yearEnd.value) {
                items += yearStart.clone()
                var maxDays = monthDayCount[yearStart.month-1]
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
            startHour = lessonRanges.map { it.startTime.hour }.min() ?: DEFAULT_START_HOUR
            endHour = lessonRanges.map { it.endTime.hour }.max()?.plus(1) ?: DEFAULT_END_HOUR
        }
        deferred.await()

        val pagerAdapter = TimetablePagerAdapter(
                fragmentManager ?: return@launch,
                items,
                startHour,
                endHour
        )
        b.viewPager.offscreenPageLimit = 2
        b.viewPager.adapter = pagerAdapter
        b.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                if (b.refreshLayout != null) {
                    b.refreshLayout.isEnabled = state == ViewPager.SCROLL_STATE_IDLE
                }
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }

            override fun onPageSelected(position: Int) {
                pageSelection = items[position]
                activity.navView.bottomBar.fabEnable = items[position].value != today
                if (activity.navView.bottomBar.fabEnable && !fabShown) {
                    activity.gainAttentionFAB()
                    fabShown = true
                }
            }
        })

        b.tabLayout.setUpWithViewPager(b.viewPager)
        b.tabLayout.setCurrentItem(items.indexOfFirst { it.value == today }, false)

        //activity.navView.bottomBar.fabEnable = true
        activity.navView.bottomBar.fabExtendedText = getString(R.string.timetable_today)
        activity.navView.bottomBar.fabIcon = CommunityMaterial.Icon.cmd_calendar_today
        activity.navView.setFabOnClickListener(View.OnClickListener {
            b.tabLayout.setCurrentItem(items.indexOfFirst { it.value == today }, true)
        })
    }}
}
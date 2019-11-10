package pl.szczodrzynski.edziennik.ui.modules.timetable.v2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager.widget.ViewPager
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.FragmentTimetableV2Binding
import pl.szczodrzynski.edziennik.utils.Themes
import pl.szczodrzynski.edziennik.utils.models.Date

class TimetableFragment : Fragment() {
    companion object {
        private const val TAG = "TimetableFragment"
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: FragmentTimetableV2Binding
    private var fabShown = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        if (context == null)
            return null
        app = activity.application as App
        context!!.theme.applyStyle(Themes.appTheme, true)
        if (app.profile == null)
            return inflater.inflate(R.layout.fragment_loading, container, false)
        // activity, context and profile is valid
        b = FragmentTimetableV2Binding.inflate(inflater)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // TODO check if app, activity, b can be null
        if (app.profile == null || !isAdded)
            return

        val items = mutableListOf<Date>()

        val monthDayCount = listOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)

        val today = Date.getToday().value
        val yearStart = app.profile.dateSemester1Start?.clone() ?: return
        val yearEnd = app.profile.dateYearEnd ?: return
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

        val pagerAdapter = TimetablePagerAdapter(fragmentManager ?: return, items)
        b.viewPager.adapter = pagerAdapter
        b.viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }

            override fun onPageSelected(position: Int) {
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
    }
}
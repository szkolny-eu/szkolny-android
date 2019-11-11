package pl.szczodrzynski.edziennik.ui.modules.timetable.v2

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Week

class TimetablePagerAdapter(val fragmentManager: FragmentManager, val items: List<Date>) : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    companion object {
        private const val TAG = "TimetablePagerAdapter"
    }

    private val today by lazy { Date.getToday() }
    private val weekStart by lazy { today.clone().stepForward(0, 0, -today.weekDay) }
    private val weekEnd by lazy { weekStart.clone().stepForward(0, 0, 6) }

    override fun getItem(position: Int): Fragment {
        return pl.szczodrzynski.edziennik.ui.modules.timetable.v2.day.TimetableDayFragment(items[position])
        /*return TimetableDayFragment().apply {
            arguments = Bundle().also {
                it.putLong("date", items[position].value.toLong())
            }
        }*/
    }

    override fun getCount(): Int {
        return items.size
    }

    override fun getPageTitle(position: Int): CharSequence? {
        val date = items[position]
        val pageTitle = StringBuilder(Week.getFullDayName(date.weekDay))
        if (date > weekEnd || date < weekStart) {
            pageTitle.append(", ").append(date.stringDm)
        }
        return pageTitle
    }
}
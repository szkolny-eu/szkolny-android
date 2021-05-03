package pl.szczodrzynski.edziennik.ui.modules.settings.contributors.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import pl.szczodrzynski.edziennik.data.api.szkolny.request.Contributor
import pl.szczodrzynski.edziennik.ui.modules.settings.contributors.ContributorsFragment
import pl.szczodrzynski.edziennik.ui.modules.settings.contributors.TranslatorsFragment

class ContributorsPagerAdapter(fm: FragmentManager, contributors: Contributor?): FragmentPagerAdapter(fm) {

    private val mContributors: Contributor?

    init {
        mContributors = contributors
    }

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> ContributorsFragment(mContributors?.contributors)
            else -> TranslatorsFragment(mContributors?.translators)
        }
    }

    override fun getCount(): Int {
        return 2
    }

    override fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> "Contributors"
            else -> "Translators"
        }
    }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-3.
 */

package pl.szczodrzynski.edziennik.ui.debug

import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import pl.szczodrzynski.edziennik.databinding.BasePagerFragmentBinding
import pl.szczodrzynski.edziennik.ui.base.fragment.PagerFragment

class LabFragment : PagerFragment<BasePagerFragmentBinding, AppCompatActivity>(
    inflater = BasePagerFragmentBinding::inflate,
), CoroutineScope {

    override fun getTabLayout() = b.tabLayout
    override fun getViewPager() = b.viewPager
    override suspend fun onCreatePages() = listOf(
        LabPageFragment() to "click me",
        LabProfileFragment() to "JSON",
        LabPlaygroundFragment() to "Playground",
    )
}

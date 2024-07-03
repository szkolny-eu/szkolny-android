/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-30.
 */

package pl.szczodrzynski.edziennik.ui.template

import android.os.Bundle
import kotlinx.coroutines.CoroutineScope
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.TemplateFragmentBinding
import pl.szczodrzynski.edziennik.ext.Bundle
import pl.szczodrzynski.edziennik.ext.addOnPageSelectedListener
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.ui.base.lazypager.FragmentLazyPagerAdapter
import pl.szczodrzynski.edziennik.ui.homework.HomeworkDate
import pl.szczodrzynski.edziennik.ui.homework.HomeworkListFragment

class TemplateFragment : BaseFragment<TemplateFragmentBinding, MainActivity>(
    inflater = TemplateFragmentBinding::inflate,
), CoroutineScope {
    companion object {
        var pageSelection = 0
    }

    override fun getRefreshLayout() = b.refreshLayout

    override suspend fun onViewCreated(savedInstanceState: Bundle?) {
        val pagerAdapter = FragmentLazyPagerAdapter(
            parentFragmentManager,
                b.refreshLayout,
                listOf(
                        HomeworkListFragment().apply {
                            arguments = Bundle("homeworkDate" to HomeworkDate.CURRENT)
                        } to getString(R.string.homework_tab_current),

                        HomeworkListFragment().apply {
                            arguments = Bundle("homeworkDate" to HomeworkDate.PAST)
                        } to getString(R.string.homework_tab_past),

                        TemplatePageFragment() to "Pager 0",
                        TemplatePageFragment() to "Pager 1",
                        TemplatePageFragment() to "Pager 2",
                        TemplatePageFragment() to "Pager 3",
                        TemplateListPageFragment() to "Pager 4",
                        TemplateListPageFragment() to "Pager 5",
                        TemplateListPageFragment() to "Pager 6",
                        TemplateListPageFragment() to "Pager 7"
                )
        )
        b.viewPager.apply {
            offscreenPageLimit = 1
            adapter = pagerAdapter
            currentItem = pageSelection
            addOnPageSelectedListener {
                pageSelection = it
            }
            b.tabLayout.setupWithViewPager(this)
        }
    }
}

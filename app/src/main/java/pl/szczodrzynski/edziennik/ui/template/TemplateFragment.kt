/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-30.
 */

package pl.szczodrzynski.edziennik.ui.template

import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.BasePagerFragmentBinding
import pl.szczodrzynski.edziennik.ext.Bundle
import pl.szczodrzynski.edziennik.ui.base.fragment.PagerFragment
import pl.szczodrzynski.edziennik.ui.homework.HomeworkDate
import pl.szczodrzynski.edziennik.ui.homework.HomeworkListFragment

class TemplateFragment : PagerFragment<BasePagerFragmentBinding, MainActivity>(
    inflater = BasePagerFragmentBinding::inflate,
) {
    companion object {
        var pageSelection = 0
    }

    override fun getTabLayout() = b.tabLayout
    override fun getViewPager() = b.viewPager

    override suspend fun onCreatePages() = listOf(
        HomeworkListFragment().apply {
            arguments = Bundle("homeworkDate" to HomeworkDate.CURRENT)
        } to getString(R.string.homework_tab_current),

        HomeworkListFragment().apply {
            arguments = Bundle("homeworkDate" to HomeworkDate.PAST)
        } to getString(R.string.homework_tab_past),

        TemplatePageFragment() to "Page 0",
        TemplatePageFragment() to "Page 1",
        TemplatePageFragment() to "Page 2",
        TemplatePageFragment() to "Page 3",
        TemplateListFragment() to "List 4",
        TemplateListFragment() to "List 5",
        TemplateListFragment() to "List 6",
        TemplateListFragment() to "List 7",
        TemplateListPageFragment() to "ListPage 8",
        TemplateListPageFragment() to "ListPage 9",
        TemplateListPageFragment() to "ListPage 10",
        TemplateListPageFragment() to "ListPage 11"
    )
}

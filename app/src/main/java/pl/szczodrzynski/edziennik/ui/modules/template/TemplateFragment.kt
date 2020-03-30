/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-30.
 */

package pl.szczodrzynski.edziennik.ui.modules.template

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.addOnPageSelectedListener
import pl.szczodrzynski.edziennik.databinding.TemplateFragmentBinding
import pl.szczodrzynski.edziennik.ui.modules.base.lazypager.FragmentLazyPagerAdapter
import pl.szczodrzynski.edziennik.ui.modules.homework.HomeworkFragment
import kotlin.coroutines.CoroutineContext

class TemplateFragment : Fragment(), CoroutineScope {
    companion object {
        private const val TAG = "TemplateFragment"
        var pageSelection = 0
    }

    private lateinit var app: App
    private lateinit var activity: MainActivity
    private lateinit var b: TemplateFragmentBinding

    private val job: Job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local/private variables go here

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity = (getActivity() as MainActivity?) ?: return null
        context ?: return null
        app = activity.application as App
        b = TemplateFragmentBinding.inflate(inflater)
        b.refreshLayout.setParent(activity.swipeRefreshLayout)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (!isAdded) return

        val pagerAdapter = FragmentLazyPagerAdapter(
                fragmentManager ?: return,
                b.refreshLayout,
                listOf(
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
                HomeworkFragment.pageSelection = it
            }
            b.tabLayout.setupWithViewPager(this)
        }
    }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-3.
 */

package pl.szczodrzynski.edziennik.ui.debug

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import pl.szczodrzynski.edziennik.databinding.TemplateFragmentBinding
import pl.szczodrzynski.edziennik.ext.addOnPageSelectedListener
import pl.szczodrzynski.edziennik.ui.base.fragment.BaseFragment
import pl.szczodrzynski.edziennik.ui.base.lazypager.FragmentLazyPagerAdapter

class LabFragment : BaseFragment<TemplateFragmentBinding, AppCompatActivity>(
    inflater = TemplateFragmentBinding::inflate,
), CoroutineScope {
    companion object {
        var pageSelection = 0
    }

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        if (!isAdded) return

        val pagerAdapter = FragmentLazyPagerAdapter(
            parentFragmentManager,
                null,
                listOf(
                        LabPageFragment() to "click me",
                        LabProfileFragment() to "JSON",
                        LabPlaygroundFragment() to "Playground",
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

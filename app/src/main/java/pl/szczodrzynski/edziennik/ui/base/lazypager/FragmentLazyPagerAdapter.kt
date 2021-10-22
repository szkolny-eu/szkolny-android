/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-30.
 */

package pl.szczodrzynski.edziennik.ui.base.lazypager

import androidx.fragment.app.FragmentManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class FragmentLazyPagerAdapter(
        fragmentManager: FragmentManager,
        swipeRefreshLayout: SwipeRefreshLayout? = null,
        val fragments: List<Pair<LazyFragment, CharSequence>>
) : LazyPagerAdapter(fragmentManager, swipeRefreshLayout) {
    override fun getPage(position: Int) = fragments[position].first
    override fun getPageTitle(position: Int) = fragments[position].second
    override fun getCount() = fragments.size
}

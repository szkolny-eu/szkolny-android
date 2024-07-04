/*
 * Copyright (c) Kuba Szczodrzyński 2024-7-3.
 */

package pl.szczodrzynski.edziennik.ui.base.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

abstract class PagerFragment<B : ViewBinding, A : AppCompatActivity>(
    inflater: ((inflater: LayoutInflater, parent: ViewGroup?, attachToParent: Boolean) -> B)?,
) : BaseFragment<B, A>(inflater) {

    private lateinit var pages: List<Pair<Fragment, String>>
    private val fragmentCache = mutableMapOf<Int, Fragment>()

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        pages = onCreatePages()

        val adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = getPageCount()
            override fun createFragment(position: Int): Fragment {
                val fragment = getPageFragment(position)
                fragmentCache[position] = fragment
                return fragment
            }
        }

        getViewPager().let {
            it.offscreenPageLimit = 1
            it.adapter = adapter
            it.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrollStateChanged(state: Int) {
                    canRefresh = when (state) {
                        ViewPager2.SCROLL_STATE_IDLE ->
                            (fragmentCache[it.currentItem] as? BaseFragment<*, *>)?.canRefresh
                            ?: false

                        else -> false
                    }
                }
            })
        }

        TabLayoutMediator(getTabLayout(), getViewPager()) { tab, position ->
            tab.text = getPageTitle(position)
        }.attach()
    }

    abstract fun getTabLayout(): TabLayout
    abstract fun getViewPager(): ViewPager2

    open suspend fun onCreatePages() = listOf<Pair<Fragment, String>>()
    open fun getPageCount() = pages.size
    open fun getPageFragment(position: Int) = pages[position].first
    open fun getPageTitle(position: Int) = pages[position].second
}

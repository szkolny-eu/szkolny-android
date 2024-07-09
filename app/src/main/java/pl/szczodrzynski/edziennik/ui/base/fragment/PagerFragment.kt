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
import kotlinx.coroutines.launch
import pl.szczodrzynski.edziennik.ext.set
import pl.szczodrzynski.edziennik.ext.setDeclaredField
import pl.szczodrzynski.edziennik.ui.base.views.RecyclerTabLayout

abstract class PagerFragment<B : ViewBinding, A : AppCompatActivity>(
    inflater: ((inflater: LayoutInflater, parent: ViewGroup?, attachToParent: Boolean) -> B)?,
) : BaseFragment<B, A>(inflater) {

    private lateinit var pages: List<Pair<Fragment, String>>
    private val fragmentCache = mutableMapOf<Int, Fragment>()

    /**
     * Stores the default page index that is activated when
     * entering the fragment. Updated every time a new page
     * is selected.
     *
     * Override with a getter and setter to make it backed by
     * e.g. app.config. Set this value before calling super.[onViewReady]
     * to provide a one-time default.
     */
    protected open var savedPageSelection = -1

    override fun getAppBars() = super.getAppBars() + listOf(
        getTabLayout(),
    )

    override suspend fun onViewReady(savedInstanceState: Bundle?) {
        if (savedPageSelection == -1)
            savedPageSelection = savedInstanceState?.getInt("pageSelection") ?: 0

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
            it.setCurrentItem(savedPageSelection, false)
            it.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageScrollStateChanged(state: Int) {
                    if (state != ViewPager2.SCROLL_STATE_IDLE) {
                        // disable swipe-to-refresh during scrolling
                        canRefreshDisabled = true
                        return
                    }
                    // take child fragment's values
                    val fragment = fragmentCache[it.currentItem] as? BaseFragment<*, *>
                    canRefreshDisabled = fragment?.canRefreshDisabled == true
                    isScrolled = fragment?.isScrolled == true
                }

                override fun onPageSelected(position: Int) {
                    savedPageSelection = position
                    launch {
                        this@PagerFragment.onPageSelected(position)
                    }
                }
            })
        }

        when (val tabLayout = getTabLayout()) {
            is TabLayout -> TabLayoutMediator(tabLayout, getViewPager()) { tab, position ->
                tab.text = getPageTitle(position)
            }.attach()

            is RecyclerTabLayout -> tabLayout.setupWithViewPager(getViewPager()) { tab, position ->
                tab.setDeclaredField("text", getPageTitle(position))
            }
        }

        onPageSelected(savedPageSelection)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState["pageSelection"] = getViewPager().currentItem
    }

    /**
     * Navigates to the specified page with a smooth scroll animation.
     *
     * To navigate without smooth scroll, use [savedPageSelection] instead
     * to provide a default page selection.
     */
    protected fun goToPage(position: Int) {
        getViewPager().setCurrentItem(position, true)
        savedPageSelection = position
        launch {
            onPageSelected(position)
        }
    }

    /**
     * Called to retrieve the [TabLayout] or [RecyclerTabLayout] view of the pager fragment.
     */
    abstract fun getTabLayout(): ViewGroup

    /**
     * Called to retrieve the [ViewPager2] view of the pager fragment.
     */
    abstract fun getViewPager(): ViewPager2

    /**
     * Called to retrieve a list of fragments (and their titles) for the pager.
     * Only used with the default implementation of [getPageCount], [getPageFragment]
     * and [getPageTitle].
     */
    open suspend fun onCreatePages() = listOf<Pair<Fragment, String>>()

    open fun getPageCount() = pages.size
    open fun getPageFragment(position: Int) = pages[position].first
    open fun getPageTitle(position: Int) = pages[position].second

    /**
     * Called when a new page is selected, by either:
     * - opening the fragment for the first time (default page),
     * - calling [goToPage], or
     * - navigating to another page by the user.
     *
     * The [savedPageSelection] is updated before this method is called.
     */
    open suspend fun onPageSelected(position: Int) {}
}

/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-29.
 */

package pl.szczodrzynski.edziennik.ui.base.lazypager

import android.content.Context
import android.util.AttributeSet
import androidx.viewpager.widget.ViewPager

class LazyViewPager @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null
) : ViewPager(context, attrs) {

    private var pageSelection = -1
    private var scrollState = 0

    init {
        addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                scrollState = state
                (adapter as? LazyPagerAdapter)?.let {
                    it.swipeRefreshLayout?.isEnabled = state == SCROLL_STATE_IDLE && it.enabledList[pageSelection, true]
                }
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }

            override fun onPageSelected(position: Int) {
                pageSelection = position
                (adapter as? LazyPagerAdapter)?.let {
                    it.swipeRefreshLayout?.isEnabled = scrollState == SCROLL_STATE_IDLE && it.enabledList[pageSelection, true]
                    val fragment = adapter?.instantiateItem(this@LazyViewPager, position)
                    val lazyFragment = fragment as? LazyFragment
                    lazyFragment?.createPage()
                }
            }
        })
    }
}

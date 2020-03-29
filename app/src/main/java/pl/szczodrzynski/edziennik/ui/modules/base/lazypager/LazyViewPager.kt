/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-29.
 */

package pl.szczodrzynski.edziennik.ui.modules.base.lazypager

import android.content.Context
import android.util.AttributeSet
import androidx.viewpager.widget.ViewPager

class LazyViewPager @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null
) : ViewPager(context, attrs) {
    init {
        addOnPageChangeListener(object : OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }

            override fun onPageSelected(position: Int) {
                if (adapter is LazyPagerAdapter) {
                    val fragment = adapter?.instantiateItem(this@LazyViewPager, position)
                    val lazyFragment = fragment as? LazyFragment
                    lazyFragment?.createPage()
                }
            }
        })
    }
}

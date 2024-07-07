/*
 * Copyright (c) Kuba Szczodrzyński 2024-7-7.
 */

package pl.szczodrzynski.edziennik.ui.base.views

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy
import pl.szczodrzynski.edziennik.ext.invokeDeclaredMethod
import pl.szczodrzynski.edziennik.ext.setDeclaredField
import kotlin.math.min

class RecyclerTabLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RecyclerView(context, attrs, defStyleAttr) {

    private val tabLayout: TabLayout = TabLayout(context, attrs)
    private lateinit var viewPager: ViewPager2
    private lateinit var viewPagerAdapter: RecyclerView.Adapter<ViewHolder>
    private lateinit var tabAdapter: Adapter
    private lateinit var tabConfigurationStrategy: TabConfigurationStrategy
    private lateinit var tab: TabLayout.Tab
    private lateinit var linearLayoutManager: LinearLayoutManager

    private var selectedTab = 0

    fun setupWithViewPager(pager: ViewPager2, strategy: TabConfigurationStrategy) {
        viewPager = pager
        viewPagerAdapter = pager.adapter!!
        tabAdapter = Adapter()
        tabConfigurationStrategy = strategy
        tab = TabLayout.Tab()
        linearLayoutManager = LinearLayoutManager(context).also {
            it.orientation = HORIZONTAL
        }

        viewPager.registerOnPageChangeCallback(OnPageChangeCallback())
        viewPagerAdapter.registerAdapterDataObserver(AdapterDataObserver())

        setWillNotDraw(false)
        setAdapter(tabAdapter)
        setHasFixedSize(true)
        layoutManager = linearLayoutManager
        itemAnimator = null
    }

    inner class Adapter : RecyclerView.Adapter<Adapter.TabViewHolder>() {
        override fun getItemCount(): Int {
            return viewPagerAdapter.itemCount
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
            return TabViewHolder(tabLayout.TabView(context))
        }

        override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
            val tabView = holder.tabView
            tabConfigurationStrategy.onConfigureTab(tab, position)
            tabView.setDeclaredField("tab", tab)
            tabView.invokeDeclaredMethod("updateTab")
            tabView.setDeclaredField("tab", null)
            tabView.isSelected = position == selectedTab
            tabView.isActivated = position == selectedTab
        }

        inner class TabViewHolder(val tabView: TabLayout.TabView) : ViewHolder(tabView)
    }

    inner class OnPageChangeCallback : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int,
        ) {
            val thisTabWidth =
                findViewHolderForLayoutPosition(position)?.itemView?.width ?: 0
            val nextTabWidth =
                findViewHolderForLayoutPosition(position + 1)?.itemView?.width ?: 0
            val tabDistance = (thisTabWidth / 2) + (nextTabWidth / 2)

            val offset = (width / 2.0f) - (thisTabWidth / 2.0f) - (tabDistance * positionOffset)
            // 'offset' is the screen position of the tab's left edge
            linearLayoutManager.scrollToPositionWithOffset(position, offset.toInt())

            val roundedPosition = Math.round(position + positionOffset)
            if (selectedTab != roundedPosition) {
                val previousTab = selectedTab
                selectedTab = roundedPosition
                adapter?.notifyItemRangeChanged(min(previousTab, selectedTab), 2)
            }
        }

        override fun onPageSelected(position: Int) {
        }

        override fun onPageScrollStateChanged(state: Int) {
        }
    }

    inner class AdapterDataObserver : RecyclerView.AdapterDataObserver() {
        override fun onChanged() {
        }

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) {
        }

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
        }

        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
        }

        override fun onStateRestorationPolicyChanged() {
        }
    }
}

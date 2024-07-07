/*
 * Copyright (c) Kuba Szczodrzyński 2024-7-7.
 */

package pl.szczodrzynski.edziennik.ui.base.views

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.drawable.updateBounds
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator.TabConfigurationStrategy
import pl.szczodrzynski.edziennik.ext.getDeclaredField
import pl.szczodrzynski.edziennik.ext.invokeDeclaredMethod
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ext.setDeclaredField
import kotlin.math.min

class RecyclerTabLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RecyclerView(context, attrs, defStyleAttr) {

    private val tabLayout: TabLayout = TabLayout(context, attrs)
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var viewPager: ViewPager2
    private lateinit var viewPagerAdapter: RecyclerView.Adapter<ViewHolder>
    private lateinit var tabAdapter: Adapter
    private lateinit var tabConfigurationStrategy: TabConfigurationStrategy
    private lateinit var tab: TabLayout.Tab
    private lateinit var tabSelectedIndicator: Drawable
    private var tabIndicatorGravity: Int = 0
    private var tabIndicatorInterpolator: Any? = null

    private var selectedTab = 0

    fun setupWithViewPager(pager: ViewPager2, strategy: TabConfigurationStrategy) {
        linearLayoutManager = LinearLayoutManager(context).also {
            it.orientation = HORIZONTAL
        }
        viewPager = pager
        viewPagerAdapter = pager.adapter!!
        tabAdapter = Adapter()
        tabConfigurationStrategy = strategy
        tab = TabLayout.Tab()
        tabSelectedIndicator = tabLayout.tabSelectedIndicator
        tabIndicatorGravity = tabLayout.tabIndicatorGravity
        tabIndicatorInterpolator = tabLayout.getDeclaredField("tabIndicatorInterpolator")

        this.addOnScrollListener(OnScrollListener())
        viewPager.registerOnPageChangeCallback(OnPageChangeCallback())
        viewPagerAdapter.registerAdapterDataObserver(AdapterDataObserver())

        setWillNotDraw(false)
        setAdapter(tabAdapter)
        setHasFixedSize(true)
        layoutManager = linearLayoutManager
        itemAnimator = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // draw the selected tab indicator
        var indicatorHeight = tabSelectedIndicator.bounds.height()
        if (indicatorHeight < 0)
            indicatorHeight = tabSelectedIndicator.intrinsicHeight

        var indicatorTop = 0
        var indicatorBottom = 0
        when (tabLayout.tabIndicatorGravity) {
            TabLayout.INDICATOR_GRAVITY_BOTTOM -> {
                indicatorTop = height - indicatorHeight
                indicatorBottom = height
            }

            TabLayout.INDICATOR_GRAVITY_CENTER -> {
                indicatorTop = (height - indicatorHeight) / 2
                indicatorBottom = (height + indicatorHeight) / 2
            }

            TabLayout.INDICATOR_GRAVITY_TOP -> {
                indicatorTop = 0
                indicatorBottom = indicatorHeight
            }

            TabLayout.INDICATOR_GRAVITY_STRETCH -> {
                indicatorTop = 0
                indicatorBottom = height
            }
        }

        tabSelectedIndicator.updateBounds(top = indicatorTop, bottom = indicatorBottom)
        tabSelectedIndicator.draw(canvas)
    }

    inner class Adapter : RecyclerView.Adapter<Adapter.TabViewHolder>() {
        override fun getItemCount() =
            viewPagerAdapter.itemCount

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            TabViewHolder(tabLayout.TabView(context)).also { holder ->
                holder.tabView.onClick {
                    viewPager.setCurrentItem(holder.lastPosition, true)
                }
            }

        override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
            val tabView = holder.tabView
            tabConfigurationStrategy.onConfigureTab(tab, position)
            tabView.setDeclaredField("tab", tab)
            tabView.invokeDeclaredMethod("updateTab")
            tabView.setDeclaredField("tab", null)
            tabView.isSelected = position == selectedTab
            tabView.isActivated = position == selectedTab
            holder.lastPosition = position
        }

        inner class TabViewHolder(
            val tabView: TabLayout.TabView,
            var lastPosition: Int = 0,
        ) : ViewHolder(tabView)
    }

    inner class OnScrollListener : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            val left = tabSelectedIndicator.bounds.left
            val right = tabSelectedIndicator.bounds.right
            tabSelectedIndicator.updateBounds(left = left - dx, right = right - dx)
        }
    }

    inner class OnPageChangeCallback : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int,
        ) {
            stopScroll()

            val thisTab = findViewHolderForLayoutPosition(position)?.itemView
            val nextTab = findViewHolderForLayoutPosition(position + 1)?.itemView

            // scroll to the currently selected tab
            val thisTabWidth = thisTab?.width ?: 0
            val nextTabWidth = nextTab?.width ?: 0
            val tabDistance = (thisTabWidth / 2) + (nextTabWidth / 2)
            val offset = (width / 2.0f) - (thisTabWidth / 2.0f) - (tabDistance * positionOffset)
            // 'offset' is the screen position of the tab's left edge
            linearLayoutManager.scrollToPositionWithOffset(position, offset.toInt())

            // update selection state of the current tab
            val roundedPosition = Math.round(position + positionOffset)
            if (selectedTab != roundedPosition) {
                val previousTab = selectedTab
                selectedTab = roundedPosition
                tabAdapter.notifyItemRangeChanged(min(previousTab, selectedTab), 2)
            }

            // update the width and position of the selected tab indicator
            tabIndicatorInterpolator?.invokeDeclaredMethod(
                name = "updateIndicatorForOffset",
                /* tabLayout = */ TabLayout::class.java to tabLayout,
                /* startTitle = */ View::class.java to thisTab,
                /* endTitle = */ View::class.java to nextTab,
                /* offset = */ Float::class.java to positionOffset,
                /* indicator = */ Drawable::class.java to tabSelectedIndicator,
            )
        }
    }

    inner class AdapterDataObserver : RecyclerView.AdapterDataObserver() {
        @SuppressLint("NotifyDataSetChanged")
        override fun onChanged() =
            tabAdapter.notifyDataSetChanged()

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) =
            tabAdapter.notifyItemRangeChanged(positionStart, itemCount)

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) =
            tabAdapter.notifyItemRangeInserted(positionStart, itemCount)

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) =
            tabAdapter.notifyItemRangeRemoved(positionStart, itemCount)

        @SuppressLint("NotifyDataSetChanged")
        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) =
            tabAdapter.notifyDataSetChanged()
    }
}

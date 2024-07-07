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

    fun setupWithViewPager(pager: ViewPager2, strategy: TabConfigurationStrategy) {
        viewPager = pager
        viewPagerAdapter = pager.adapter!!
        tabAdapter = Adapter()
        tabConfigurationStrategy = strategy
        tab = TabLayout.Tab()

        viewPager.registerOnPageChangeCallback(OnPageChangeCallback())
        viewPagerAdapter.registerAdapterDataObserver(AdapterDataObserver())

        setWillNotDraw(false)
        setAdapter(tabAdapter)
        setHasFixedSize(true)
        layoutManager = LinearLayoutManager(context).also {
            it.orientation = HORIZONTAL
        }
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
        }

        inner class TabViewHolder(val tabView: TabLayout.TabView) : ViewHolder(tabView)
    }

    inner class OnPageChangeCallback : ViewPager2.OnPageChangeCallback() {
        override fun onPageScrolled(
            position: Int,
            positionOffset: Float,
            positionOffsetPixels: Int,
        ) {
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

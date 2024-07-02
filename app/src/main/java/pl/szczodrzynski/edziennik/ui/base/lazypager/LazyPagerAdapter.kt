/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-29.
 */

package pl.szczodrzynski.edziennik.ui.base.lazypager

import android.util.SparseBooleanArray
import androidx.core.util.set
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

abstract class LazyPagerAdapter(fragmentManager: FragmentManager, val swipeRefreshLayout: SwipeRefreshLayout? = null) : FragmentStatePagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    internal val enabledList = SparseBooleanArray()
    private val refreshLayoutCallback: (position: Int, isEnabled: Boolean) -> Unit = { position, isEnabled ->
        swipeRefreshLayout?.isEnabled = isEnabled
        if (position > -1)
            enabledList[position] = isEnabled
    }
    final override fun getItem(position: Int): LazyFragment<*, *> {
        return getPage(position).also {
            it.position = position
            it.swipeRefreshLayoutCallback = refreshLayoutCallback
        }
    }
    abstract fun getPage(position: Int): LazyFragment<*, *>
    abstract override fun getPageTitle(position: Int): CharSequence
}

/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-29.
 */

package pl.szczodrzynski.edziennik.ui.base.lazypager

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView

abstract class LazyFragment : Fragment() {
    private var isPageCreated = false
    internal var position = -1
    internal var swipeRefreshLayoutCallback: ((position: Int, isEnabled: Boolean) -> Unit)? = null
    internal var onPageDestroy: ((position: Int, outState: Bundle?) -> Unit?)? = null

    /**
     * Called when the page is first shown, or if previous
     * [onPageCreated] returned false
     *
     * @return true if the view is set up
     * @return false if the setup failed. The method may be then called
     * again, when page becomes visible.
     */
    abstract fun onPageCreated(): Boolean

    fun enableSwipeToRefresh() = swipeRefreshLayoutCallback?.invoke(position, true)
    fun disableSwipeToRefresh() = swipeRefreshLayoutCallback?.invoke(position, false)
    fun setSwipeToRefresh(enabled: Boolean) = swipeRefreshLayoutCallback?.invoke(position, enabled)

    val onScrollListener: RecyclerView.OnScrollListener
        get() = object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (recyclerView.canScrollVertically(-1))
                    disableSwipeToRefresh()
                if (!recyclerView.canScrollVertically(-1) && newState == RecyclerView.SCROLL_STATE_IDLE)
                    enableSwipeToRefresh()
            }
        }

    internal fun createPage() {
        if (!isPageCreated && isAdded) {
            isPageCreated = onPageCreated()
        }
    }

    override fun onDestroyView() {
        isPageCreated = false
        super.onDestroyView()
    }

    override fun onResume() {
        createPage()
        super.onResume()
    }
}

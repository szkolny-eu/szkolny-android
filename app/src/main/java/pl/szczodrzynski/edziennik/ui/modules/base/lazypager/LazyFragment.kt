/*
 * Copyright (c) Kuba Szczodrzyński 2020-3-29.
 */

package pl.szczodrzynski.edziennik.ui.modules.base.lazypager

import androidx.fragment.app.Fragment

abstract class LazyFragment : Fragment() {
    private var isPageCreated = false
    internal var position = -1
    internal var swipeRefreshLayoutCallback: ((position: Int, isEnabled: Boolean) -> Unit)? = null

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

    internal fun createPage() {
        if (!isPageCreated && isAdded) {
            isPageCreated = onPageCreated()
        }
    }

    override fun onResume() {
        createPage()
        super.onResume()
    }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-22.
 */

package pl.szczodrzynski.edziennik.ui.modules.base

import androidx.fragment.app.Fragment

abstract class PagerFragment : Fragment() {
    private var isPageCreated = false

    /**
     * Called when the page is first shown, or if previous
     * [onPageCreated] returned false
     *
     * @return true if the view is set up
     * @return false if the setup failed. The method may be then called
     * again, when page becomes visible.
     */
    abstract fun onPageCreated(): Boolean

    override fun onResume() {
        if (!isPageCreated) {
            isPageCreated = onPageCreated()
        }
        super.onResume()
    }
}

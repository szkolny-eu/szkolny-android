/*
 * Copyright (c) Kuba Szczodrzyński 2024-7-9.
 */

package pl.szczodrzynski.edziennik.ui.base.fragment

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView

@SuppressLint("ClickableViewAccessibility")
internal fun BaseFragment<*, *>.setupScrollingView() {
    when (val view = getRefreshScrollingView()) {
        is RecyclerView -> {
            canRefresh = !view.canScrollVertically(-1)
            view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    // disable refresh when scrolled down
                    if (recyclerView.canScrollVertically(-1))
                        canRefresh = false
                    // enable refresh when scrolled to the top and not scrolling anymore
                    else if (newState == RecyclerView.SCROLL_STATE_IDLE)
                        canRefresh = true
                }
            })
        }

        is View -> {
            canRefresh = !view.canScrollVertically(-1)
            var isTouched = false
            view.setOnTouchListener { _, event ->
                // keep track of the touch state
                when (event.action) {
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isTouched = false
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> isTouched = true
                }
                // disable refresh when scrolled down
                if (view.canScrollVertically(-1))
                    canRefresh = false
                // enable refresh when scrolled to the top and not touching anymore
                else if (!isTouched)
                    canRefresh = true
                false
            }
        }

        else -> {
            // dispatch the default value to the activity
            canRefresh = canRefresh
        }
    }
}

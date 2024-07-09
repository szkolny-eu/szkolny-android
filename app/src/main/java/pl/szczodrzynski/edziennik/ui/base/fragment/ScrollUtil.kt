/*
 * Copyright (c) Kuba Szczodrzyński 2024-7-9.
 */

package pl.szczodrzynski.edziennik.ui.base.fragment

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.MainActivity

@SuppressLint("ClickableViewAccessibility")
internal fun BaseFragment<*, *>.setupScrollingView(setIsScrolled: (Boolean) -> Unit) {
    when (val view = getRefreshScrollingView()) {
        is RecyclerView -> {
            setIsScrolled(view.canScrollVertically(-1))
            view.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    if (recyclerView.canScrollVertically(-1))
                        setIsScrolled(true)
                    else if (newState == RecyclerView.SCROLL_STATE_IDLE)
                        setIsScrolled(false)
                }
            })
        }

        is View -> {
            setIsScrolled(view.canScrollVertically(-1))
            var isTouched = false
            view.setOnTouchListener { _, event ->
                // keep track of the touch state
                when (event.action) {
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> isTouched = false
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> isTouched = true
                }
                if (view.canScrollVertically(-1))
                    setIsScrolled(true)
                else if (!isTouched)
                    setIsScrolled(false)
                false
            }
        }

        else -> {
            // dispatch the default value to the activity
            setIsScrolled(false)
        }
    }
}

internal fun BaseFragment<*, *>.dispatchCanRefresh() {
    (activity as? MainActivity)?.swipeRefreshLayout?.isEnabled =
        !canRefreshDisabled && !isScrolled
}

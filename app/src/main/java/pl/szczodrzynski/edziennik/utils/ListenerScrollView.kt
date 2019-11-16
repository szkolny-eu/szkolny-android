/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-16.
 */

package pl.szczodrzynski.edziennik.utils

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ScrollView

class ListenerScrollView(
        context: Context,
        attrs: AttributeSet? = null
) : ScrollView(context, attrs) {

    private var onScrollChangedListener: ((v: ListenerScrollView, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) -> Unit)? = null
    private var onRefreshLayoutEnabledListener: ((enabled: Boolean) -> Unit)? = null
    private var refreshLayoutEnabled = true

    init {
        setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                refreshLayoutEnabled = scrollY < 10
                onRefreshLayoutEnabledListener?.invoke(refreshLayoutEnabled)
            }
            false
        }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        onScrollChangedListener?.invoke(this, l, t, oldl, oldt)
        if (t > 10 && refreshLayoutEnabled) {
            refreshLayoutEnabled = false
            onRefreshLayoutEnabledListener?.invoke(refreshLayoutEnabled)
        }
    }

    fun setOnScrollChangedListener(l: ((v: ListenerScrollView, scrollX: Int, scrollY: Int, oldScrollX: Int, oldScrollY: Int) -> Unit)?) {
        onScrollChangedListener = l
    }

    fun setOnRefreshLayoutEnabledListener(l: ((enabled: Boolean) -> Unit)?) {
        onRefreshLayoutEnabledListener = l
    }
}
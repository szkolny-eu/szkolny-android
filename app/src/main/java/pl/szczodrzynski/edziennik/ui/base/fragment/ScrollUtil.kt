/*
 * Copyright (c) Kuba Szczodrzyński 2024-7-9.
 */

package pl.szczodrzynski.edziennik.ui.base.fragment

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import com.google.android.material.motion.MotionUtils
import com.google.android.material.shape.MaterialShapeDrawable
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ext.resolveAttr

@SuppressLint("ClickableViewAccessibility")
internal fun BaseFragment<*, *>.setupScrollListener(setIsScrolled: (Boolean) -> Unit) {
    when (val view = getScrollingView()) {
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

internal class AppBarColorAnimator(
    context: Context,
    private val bars: List<View>,
) : ValueAnimator.AnimatorUpdateListener {
    companion object {
        // keep track of the current animation value applied globally to the view
        private var currentValue = 0.0f
    }

    private lateinit var animator: ValueAnimator

    private val barColor = R.attr.colorSurface.resolveAttr(context)
    private val liftColor = R.attr.colorSurfaceContainer.resolveAttr(context)

    context(BaseFragment<*, *>)
    fun dispatchLiftOnScroll() {
        if (::animator.isInitialized)
            animator.cancel()
        animator = ValueAnimator.ofFloat(
            currentValue,
            if (isScrolled) 1.0f else 0.0f,
        )
        animator.duration = MotionUtils.resolveThemeDuration(
            activity,
            R.attr.motionDurationMedium2,
            resources.getInteger(R.integer.app_bar_elevation_anim_duration),
        ).toLong()
        animator.interpolator = MotionUtils.resolveThemeInterpolator(
            activity,
            R.attr.motionEasingStandardInterpolator,
            LinearInterpolator(),
        )
        animator.addUpdateListener(this)
        animator.start()
    }

    override fun onAnimationUpdate(animation: ValueAnimator) {
        currentValue = animation.animatedValue as Float
        val mixedColor = MaterialColors.layer(
            barColor,
            liftColor,
            currentValue,
        )
        for (bar in bars) {
            (bar.background as? MaterialShapeDrawable)?.fillColor =
                ColorStateList.valueOf(mixedColor)
        }
    }
}

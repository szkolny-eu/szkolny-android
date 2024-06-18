package pl.szczodrzynski.navlib

import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.ScaleAnimation
import android.view.animation.Transformation
import android.widget.LinearLayout

object Anim {
    fun expand(v: View, duration: Int?, animationListener: Animation.AnimationListener?) {
        v.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val targetHeight = v.measuredHeight
        //Log.d("Anim", "targetHeight="+targetHeight);
        v.visibility = View.VISIBLE
        v.layoutParams.height = 0
        val a = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                v.layoutParams.height = if (interpolatedTime == 1.0f)
                    LinearLayout.LayoutParams.WRAP_CONTENT//(int)(targetHeight * interpolatedTime)
                else
                    (targetHeight * interpolatedTime).toInt()
                v.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }
        if (duration == null) {
            a.duration = (targetHeight.toFloat() / v.context.resources.displayMetrics.density).toInt().toLong()
        } else {
            a.duration = duration as Long
        }
        if (animationListener != null) {
            a.setAnimationListener(animationListener)
        }
        v.startAnimation(a)
    }

    fun collapse(v: View, duration: Int?, animationListener: Animation.AnimationListener?) {
        val initialHeight = v.measuredHeight
        val a = object : Animation() {
            override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                if (interpolatedTime == 1.0f) {
                    v.visibility = View.GONE
                    return
                }
                v.layoutParams.height = initialHeight - (initialHeight.toFloat() * interpolatedTime).toInt()
                v.requestLayout()
            }

            override fun willChangeBounds(): Boolean {
                return true
            }
        }
        if (duration == null) {
            a.duration = (initialHeight.toFloat() / v.context.resources.displayMetrics.density).toInt().toLong()
        } else {
            a.duration = duration as Long
        }
        if (animationListener != null) {
            a.setAnimationListener(animationListener)
        }
        v.startAnimation(a)
    }

    fun fadeIn(v: View, duration: Int?, animationListener: Animation.AnimationListener?) {
        val fadeIn = AlphaAnimation(0f, 1f)
        fadeIn.interpolator = DecelerateInterpolator() //add this
        fadeIn.duration = duration!!.toLong()
        fadeIn.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                v.visibility = View.VISIBLE
                animationListener?.onAnimationStart(animation)
            }

            override fun onAnimationEnd(animation: Animation) {
                animationListener?.onAnimationEnd(animation)
            }

            override fun onAnimationRepeat(animation: Animation) {
                animationListener?.onAnimationRepeat(animation)
            }
        })
        v.startAnimation(fadeIn)
    }

    fun fadeOut(v: View, duration: Int?, animationListener: Animation.AnimationListener?) {
        val fadeOut = AlphaAnimation(1f, 0f)
        fadeOut.interpolator = AccelerateInterpolator() //and this
        fadeOut.duration = duration!!.toLong()
        fadeOut.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                animationListener?.onAnimationStart(animation)
            }

            override fun onAnimationEnd(animation: Animation) {
                v.visibility = View.INVISIBLE
                animationListener?.onAnimationEnd(animation)
            }

            override fun onAnimationRepeat(animation: Animation) {
                animationListener?.onAnimationRepeat(animation)
            }
        })
        v.startAnimation(fadeOut)
    }

    fun scaleView(
        v: View,
        duration: Int?,
        animationListener: Animation.AnimationListener?,
        startScale: Float,
        endScale: Float
    ) {
        val anim = ScaleAnimation(
            1f, 1f, // Start and end values for the X axis scaling
            startScale, endScale, // Start and end values for the Y axis scaling
            Animation.RELATIVE_TO_SELF, 0f, // Pivot point of X scaling
            Animation.RELATIVE_TO_SELF, 0f
        ) // Pivot point of Y scaling
        anim.fillAfter = true // Needed to keep the result of the animation
        anim.duration = duration!!.toLong()
        anim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation) {
                animationListener?.onAnimationStart(animation)
            }

            override fun onAnimationEnd(animation: Animation) {
                animationListener?.onAnimationEnd(animation)
            }

            override fun onAnimationRepeat(animation: Animation) {
                animationListener?.onAnimationRepeat(animation)
            }
        })
        v.startAnimation(anim)
    }

    class ResizeAnimation(
        private val mView: View,
        private val mFromWidth: Float,
        private val mFromHeight: Float,
        private val mToWidth: Float,
        private val mToHeight: Float
    ) : Animation() {

        private val width: Float
        private val height: Float

        init {
            width = mView.width.toFloat()
            height = mView.height.toFloat()
            duration = 300
        }

        override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
            val height = (mToHeight - mFromHeight) * interpolatedTime + mFromHeight
            val width = (mToWidth - mFromWidth) * interpolatedTime + mFromWidth
            val p = mView.layoutParams
            p.width = (width * this.width).toInt()
            p.height = (height * this.height).toInt()
            mView.requestLayout()
        }
    }
}

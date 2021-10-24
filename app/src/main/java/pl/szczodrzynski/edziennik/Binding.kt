/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-11.
 */
package pl.szczodrzynski.edziennik

import android.graphics.Paint
import android.widget.TextView
import androidx.databinding.BindingAdapter
import pl.szczodrzynski.edziennik.ext.dp

object Binding {
    @JvmStatic
    @BindingAdapter("strikeThrough")
    fun strikeThrough(textView: TextView, strikeThrough: Boolean) {
        if (strikeThrough) {
            textView.paintFlags = textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            textView.paintFlags = textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }
    }

    private fun resizeDrawable(textView: TextView, index: Int, size: Int) {
        val drawables = textView.compoundDrawables
        drawables[index]?.setBounds(0, 0, size, size)
        textView.setCompoundDrawables(drawables[0], drawables[1], drawables[2], drawables[3])
    }

    @JvmStatic
    @BindingAdapter("android:drawableLeftAutoSize")
    fun drawableLeftAutoSize(textView: TextView, enable: Boolean) = resizeDrawable(
        textView,
        index = 0,
        size = textView.textSize.toInt(),
    )

    @JvmStatic
    @BindingAdapter("android:drawableRightAutoSize")
    fun drawableRightAutoSize(textView: TextView, enable: Boolean) = resizeDrawable(
        textView,
        index = 2,
        size = textView.textSize.toInt(),
    )

    @JvmStatic
    @BindingAdapter("android:drawableLeftSize")
    fun drawableLeftSize(textView: TextView, sizeDp: Int) = resizeDrawable(
        textView,
        index = 0,
        size = sizeDp.dp,
    )

    @JvmStatic
    @BindingAdapter("android:drawableTopSize")
    fun drawableTopSize(textView: TextView, sizeDp: Int) = resizeDrawable(
        textView,
        index = 1,
        size = sizeDp.dp,
    )

    @JvmStatic
    @BindingAdapter("android:drawableRightSize")
    fun drawableRightSize(textView: TextView, sizeDp: Int) = resizeDrawable(
        textView,
        index = 2,
        size = sizeDp.dp,
    )

    @JvmStatic
    @BindingAdapter("android:drawableBottomSize")
    fun drawableBottomSize(textView: TextView, sizeDp: Int) = resizeDrawable(
        textView,
        index = 3,
        size = sizeDp.dp,
    )
}

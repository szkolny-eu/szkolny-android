/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-17.
 */

package pl.szczodrzynski.edziennik.ext

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat

fun colorFromName(name: String?): Int {
    val i = (name ?: "").crc32()
    return when ((i / 10 % 16 + 1).toInt()) {
        13 -> 0xffF44336
        4  -> 0xffF50057
        2  -> 0xffD500F9
        9  -> 0xff6200EA
        5  -> 0xffFFAB00
        1  -> 0xff304FFE
        6  -> 0xff40C4FF
        14 -> 0xff26A69A
        15 -> 0xff00C853
        7  -> 0xffFFD600
        3  -> 0xffFF3D00
        8  -> 0xffDD2C00
        10 -> 0xff795548
        12 -> 0xff2979FF
        11 -> 0xffFF6D00
        else -> 0xff64DD17
    }.toInt()
}

fun colorFromCssName(name: String): Int {
    return when (name) {
        "red" -> 0xffff0000
        "green" -> 0xff008000
        "blue" -> 0xff0000ff
        "violet" -> 0xffee82ee
        "brown" -> 0xffa52a2a
        "orange" -> 0xffffa500
        "black" -> 0xff000000
        "white" -> 0xffffffff
        else -> -1L
    }.toInt()
}

@ColorInt
fun @receiver:AttrRes Int.resolveAttr(context: Context?): Int {
    val typedValue = TypedValue()
    context?.theme?.resolveAttribute(this, typedValue, true)
    return typedValue.data
}
@ColorInt
fun @receiver:ColorRes Int.resolveColor(context: Context): Int {
    return ResourcesCompat.getColor(context.resources, this, context.theme)
}
fun @receiver:DrawableRes Int.resolveDrawable(context: Context): Drawable {
    return ResourcesCompat.getDrawable(context.resources, this, context.theme)!!
}

fun Int.toColorStateList(): ColorStateList {
    val states = arrayOf(
        intArrayOf( android.R.attr.state_enabled ),
        intArrayOf(-android.R.attr.state_enabled ),
        intArrayOf(-android.R.attr.state_checked ),
        intArrayOf( android.R.attr.state_pressed )
    )

    val colors = intArrayOf(
        this,
        this,
        this,
        this
    )

    return ColorStateList(states, colors)
}

fun Drawable.setTintColor(color: Int): Drawable {
    colorFilter = PorterDuffColorFilter(
        color,
        PorterDuff.Mode.SRC_ATOP
    )
    return this
}

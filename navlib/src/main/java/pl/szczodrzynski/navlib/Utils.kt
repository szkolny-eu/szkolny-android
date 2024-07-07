package pl.szczodrzynski.navlib

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import com.google.android.material.elevation.ElevationOverlayProvider
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.utils.colorInt
import com.mikepenz.materialdrawer.holder.StringHolder
import com.mikepenz.materialdrawer.model.interfaces.Badgeable
import com.mikepenz.materialdrawer.model.interfaces.withBadge
import com.mikepenz.materialdrawer.util.getDrawerItem
import com.mikepenz.materialdrawer.util.updateItem
import com.mikepenz.materialdrawer.widget.MaterialDrawerSliderView

fun blendColors(background: Int, foreground: Int): Int {
    val r1 = (background shr 16 and 0xff)
    val g1 = (background shr 8 and 0xff)
    val b1 = (background and 0xff)

    val r2 = (foreground shr 16 and 0xff)
    val g2 = (foreground shr 8 and 0xff)
    val b2 = (foreground and 0xff)
    val a2 = (foreground shr 24 and 0xff)

    val factor = a2.toFloat() / 255f
    val red = (r1 * (1 - factor) + r2 * factor)
    val green = (g1 * (1 - factor) + g2 * factor)
    val blue = (b1 * (1 - factor) + b2 * factor)

    return (0xff000000 or (red.toLong() shl 16) or (green.toLong() shl 8) or (blue.toLong())).toInt()
}

fun elevateSurface(context: Context, dp: Int): Int {
    ElevationOverlayProvider(context).apply {
        return compositeOverlay(themeSurfaceColor, dp * context.resources.displayMetrics.density)
    }
}

fun IconicsDrawable.colorAttr(context: Context, @AttrRes attrRes: Int) {
    colorInt = getColorFromAttr(context, attrRes)
}

fun getColorFromAttr(context: Context, @AttrRes color: Int): Int {
    val typedValue = TypedValue()
    context.theme.resolveAttribute(color, typedValue, true)
    if (typedValue.resourceId != 0) {
        return ContextCompat.getColor(context, typedValue.resourceId)
    }
    return typedValue.data
}

fun Drawable.setBadgeCount(count: Int) {
    if (this is LayerDrawable) {
        (this as LayerDrawable?)?.apply {
            findDrawableByLayerId(R.id.ic_badge)
                .takeIf { it is BadgeDrawable }
                ?.also { badge ->
                    (badge as BadgeDrawable).setCount(count.toString())
                    mutate()
                    setDrawableByLayerId(R.id.ic_badge, badge)
                }
        }
    }
}

fun MaterialDrawerSliderView.updateBadge(identifier: Long, badge: StringHolder?) {
    val drawerItem = getDrawerItem(identifier)
    if (drawerItem is Badgeable) {
        drawerItem.withBadge(badge)
        updateItem(drawerItem)
    }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2024-6-20.
 */

package pl.szczodrzynski.navlib

import android.graphics.drawable.LayerDrawable
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.navlibfont.NavLibFont
import com.mikepenz.iconics.utils.sizeDp
import pl.szczodrzynski.navlib.bottomsheet.NavBottomSheet

interface NavMenuBarBase {

    var drawerClickListener: (() -> Unit)?
    var menuClickListener: (() -> Unit)?

    var Toolbar.enableMenuControls: Boolean
        get() = navigationIcon != null
        set(value) {
            if (value)
                this.attach()
            else
                this.detach()
        }

    private fun Toolbar.attach() {
        val navIcon = ContextCompat.getDrawable(context, R.drawable.ic_menu_badge) as LayerDrawable?
        navIcon?.apply {
            mutate()
            setDrawableByLayerId(R.id.ic_menu, IconicsDrawable(context).apply {
                this.icon = NavLibFont.Icon.nav_menu
                sizeDp = 24
                colorAttr(context, R.attr.colorOnSurface)
            })
            setDrawableByLayerId(R.id.ic_badge, BadgeDrawable(context))
        }

        navigationIcon = navIcon
        setNavigationOnClickListener {
            drawerClickListener?.invoke()
        }

        menu.add(0, -1, 0, "Menu")
            .setIcon(IconicsDrawable(context).apply {
                this.icon = NavLibFont.Icon.nav_dots_vertical
                sizeDp = 24
                colorAttr(context, R.attr.colorOnSurface)
            })
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

        setOnMenuItemClickListener {
            menuClickListener?.invoke()
            true
        }
    }

    private fun Toolbar.detach() {
        navigationIcon = null
        setNavigationOnClickListener(null)
        menu.clear()
        setOnMenuItemClickListener(null)
    }
}

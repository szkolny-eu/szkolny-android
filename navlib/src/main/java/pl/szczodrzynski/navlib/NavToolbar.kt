package pl.szczodrzynski.navlib

import android.content.Context
import android.graphics.drawable.LayerDrawable
import android.util.AttributeSet
import android.view.MenuItem
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.google.android.material.appbar.MaterialToolbar
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.library.navlibfont.NavLibFont
import com.mikepenz.iconics.utils.sizeDp
import pl.szczodrzynski.navlib.bottomsheet.NavBottomSheet

class NavToolbar : MaterialToolbar {
    constructor(context: Context) : super(context) {
        create(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        create(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        create(attrs, defStyle)
    }

    var toolbarImage: ImageView? = null
        set(value) {
            field = value
            toolbarImage?.setOnClickListener {
                profileImageClickListener?.invoke()
            }
        }

    var bottomSheet: NavBottomSheet? = null

    private fun create(attrs: AttributeSet?, defStyle: Int) {
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

        super.setOnMenuItemClickListener {
            if (it.itemId == -1 && bottomSheet?.enable == true) {
                bottomSheet?.toggle()
            }
            else {
                onMenuItemClickListener?.onMenuItemClick(it)
            }
            true
        }
    }

    var profileImageClickListener: (() -> Unit)? = null
    var drawerClickListener: (() -> Unit)? = null

    private var onMenuItemClickListener: OnMenuItemClickListener? = null
    override fun setOnMenuItemClickListener(listener: OnMenuItemClickListener?) {
        onMenuItemClickListener = listener
    }

    var profileImage
        get() = toolbarImage?.drawable
        set(value) {
            toolbarImage?.setImageDrawable(value)
        }
}
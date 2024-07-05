package pl.szczodrzynski.navlib

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.bottomappbar.BottomAppBar
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.utils.sizeDp
import pl.szczodrzynski.navlib.bottomsheet.NavBottomSheet

class NavBottomBar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : BottomAppBar(context, attrs, defStyle), NavMenuBarBase {

    internal lateinit var navView: NavView
    override lateinit var bottomSheet: NavBottomSheet
    internal lateinit var fabExtendedView: ExtendedFloatingActionButton

    /**
     * Shows the BottomAppBar and sets the contentView's margin to be
     * above the BottomAppBar.
     */
    var enable
        get() = isVisible
        set(value) {
            isVisible = value
            setFabParams()
            navView.setContentMargins()
        }

    /**
     * Whether the FAB should be visible.
     */
    var fabEnable = true
        set(value) {
            field = value
            setFabVisibility()
        }

    /**
     * If BottomAppBar is enabled, sets its fabAlignmentMode.
     * Else, sets the actual FAB's gravity.
     */
    var fabGravity = Gravity.CENTER
        set(value) {
            field = value
            setFabParams()
        }

    /**
     * Whether the FAB should be extended and its text visible.
     */
    var fabExtended = false
        set(value) {
            field = value
            if (fabExtended)
                fabExtendedView.extend()
            else
                fabExtendedView.shrink()
        }

    /**
     * Set the FAB's icon.
     */
    var fabIcon: IIcon? = null
        set(value) {
            field = value
            fabExtendedView.icon = IconicsDrawable(context).apply {
                icon = value
                colorAttr(context, R.attr.colorOnPrimaryContainer)
                sizeDp = 24
            }
        }

    /**
     * Set the ExtendedFAB's text.
     */
    var fabExtendedText
        get() = fabExtendedView.text
        set(value) {
            fabExtendedView.text = value
        }

    /**
     * Set the FAB's on click listener
     */
    fun setFabOnClickListener(onClickListener: OnClickListener?) {
        fabExtendedView.setOnClickListener(onClickListener)
    }

    override var drawerClickListener: (() -> Unit)? = null
    override var menuClickListener: (() -> Unit)? = null

    init {
        setOnTouchListener(bottomSheet::dispatchBottomBarEvent)
        elevation = 0f
    }

    private fun setFabParams() {
        fabExtendedView.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            if (enable) {
                anchorId = this@NavBottomBar.id
                anchorGravity = fabGravity or Gravity.TOP
                gravity = Gravity.NO_GRAVITY
            } else {
                anchorId = View.NO_ID
                anchorGravity = Gravity.NO_GRAVITY
                gravity = fabGravity or Gravity.BOTTOM
            }
            fabAlignmentMode =
                if (fabGravity == Gravity.END)
                    FAB_ALIGNMENT_MODE_END
                else
                    FAB_ALIGNMENT_MODE_CENTER
        }
        setFabVisibility()
    }

    private fun setFabVisibility() =
        if (fabEnable)
            fabExtendedView.show()
        else
            fabExtendedView.hide()
}

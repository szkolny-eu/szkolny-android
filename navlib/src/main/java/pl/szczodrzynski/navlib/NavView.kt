package pl.szczodrzynski.navlib

import android.content.Context
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.updateLayoutParams
import pl.szczodrzynski.navlib.bottomsheet.NavBottomSheet
import pl.szczodrzynski.navlib.databinding.NavViewBinding
import pl.szczodrzynski.navlib.drawer.NavDrawer

class NavView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : FrameLayout(context, attrs, defStyle) {

    private val b = NavViewBinding.inflate(LayoutInflater.from(context), this)
    private var contentView: LinearLayout? = null
    val drawer: NavDrawer

    val coordinator
        get() = b.nvCoordinator
    val toolbar
        get() = b.nvToolbar
    val bottomBar
        get() = b.nvBottomBar
    val nightlyText
        get() = b.nvNightlyText
    val bottomSheet
        get() = b.nvBottomSheet

    init {
        contentView = b.nvContent

        drawer = NavDrawer(
            context,
            b.nvDrawerLayout,
            b.nvDrawerContainerLandscape,
            b.nvMiniDrawerContainerPortrait,
            b.nvMiniDrawerElevation,
        )

        drawer.toolbar = b.nvToolbar
        drawer.bottomBar = b.nvBottomBar

        b.nvToolbar.navView = this
        b.nvToolbar.toolbarImage = b.nvToolbarImage

        b.nvBottomBar.navView = this
        b.nvBottomBar.fabExtendedView = b.nvExtendedFloatingActionButton

        toolbar.drawerClickListener = drawer::open
        toolbar.menuClickListener = bottomSheet::open
        bottomBar.drawerClickListener = drawer::open
        bottomBar.menuClickListener = bottomSheet::open

        b.nvBottomBar.setOnTouchListener(bottomSheet::dispatchBottomSheetEvent)
        b.nvBottomSheet.scrimView.setOnTouchListener(bottomSheet::dispatchBottomSheetEvent)
    }

    fun configSystemBarsUtil(systemBarsUtil: SystemBarsUtil) {
        this.systemBarsUtil = systemBarsUtil.apply {
            this.statusBarBgView = b.nvStatusBarBackground
            this.navigationBarBgView = b.nvNavigationBarBackground
            this.statusBarDarkView = b.nvStatusBarDarker
            this.navigationBarDarkView = b.nvNavigationBarBackground
            this.insetsListener = b.nvDrawerLayout
            this.marginBySystemBars = b.nvMain
        }
    }

    /**
     * Set the FAB's on click listener
     */
    fun setFabOnClickListener(onClickListener: OnClickListener?) {
        bottomBar.setFabOnClickListener(onClickListener)
    }

    internal var systemBarsUtil: SystemBarsUtil? = null

    internal fun setContentMargins() {
        contentView?.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            topMargin = if (toolbar.enable) {
                toolbar.measure(MATCH_PARENT, WRAP_CONTENT)
                toolbar.measuredHeight
            } else 0
            bottomMargin = if (bottomBar.enable) {
                bottomBar.measure(MATCH_PARENT, WRAP_CONTENT)
                bottomBar.measuredHeight
            } else 0
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)

        Log.d(
            "NavLib",
            "CONFIGURATION CHANGED: ${newConfig?.screenWidthDp}x${newConfig?.screenHeightDp} " + if (newConfig?.orientation == ORIENTATION_PORTRAIT) "portrait" else "landscape"
        )

        systemBarsUtil?.commit()

        drawer.decideDrawerMode(
            newConfig?.orientation ?: ORIENTATION_PORTRAIT,
            newConfig?.screenWidthDp ?: 0,
            newConfig?.screenHeightDp ?: 0
        )
    }

    fun onBackPressed(): Boolean {
        if (drawer.isOpen && !drawer.fixedDrawerEnabled()) {
            if (drawer.profileSelectionIsOpen) {
                drawer.profileSelectionClose()
                return true
            }
            drawer.close()
            return true
        }
        if (bottomSheet.isOpen) {
            bottomSheet.close()
            return true
        }
        return false
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) =
        contentView?.addView(child, index, params) ?: super.addView(child, index, params)
}

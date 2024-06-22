package pl.szczodrzynski.navlib

import android.content.Context
import android.content.res.Configuration
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.graphics.Point
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
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import pl.szczodrzynski.navlib.bottomsheet.NavBottomSheet
import pl.szczodrzynski.navlib.databinding.NavViewBinding
import pl.szczodrzynski.navlib.drawer.NavDrawer


class NavView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0,
) : FrameLayout(context, attrs, defStyle) {
    companion object {
        const val SOURCE_OTHER = 0
        const val SOURCE_DRAWER = 1
        const val SOURCE_BOTTOM_SHEET = 1
    }

    private var contentView: LinearLayout? = null
    private val statusBarBackground: View
    private val navigationBarBackground: View
    private val mainView: LinearLayout
    private val floatingActionButton: FloatingActionButton
    private val extendedFloatingActionButton: ExtendedFloatingActionButton

    val coordinator: CoordinatorLayout
    val drawer: NavDrawer
    val toolbar: NavToolbar
    val bottomBar: NavBottomBar
    val nightlyText: TextView
    val bottomSheet: NavBottomSheet

    init {
        val layoutInflater = LayoutInflater.from(context)
        layoutInflater.inflate(R.layout.nav_view, this)

        contentView = findViewById(R.id.nv_content)
        statusBarBackground = findViewById(R.id.nv_statusBarBackground)
        navigationBarBackground = findViewById(R.id.nv_navigationBarBackground)
        mainView = findViewById(R.id.nv_main)
        floatingActionButton = findViewById(R.id.nv_floatingActionButton)
        extendedFloatingActionButton = findViewById(R.id.nv_extendedFloatingActionButton)

        coordinator = findViewById(R.id.nv_coordinator)
        drawer = NavDrawer(
            context,
            findViewById(R.id.nv_drawerLayout),
            findViewById(R.id.nv_drawerContainerLandscape),
            findViewById(R.id.nv_miniDrawerContainerPortrait),
            findViewById(R.id.nv_miniDrawerElevation)
        )

        toolbar = findViewById(R.id.nv_toolbar)
        bottomBar = findViewById(R.id.nv_bottomBar)
        nightlyText = findViewById(R.id.nv_nightlyText)
        bottomSheet = findViewById(R.id.nv_bottomSheet)

        drawer.toolbar = toolbar
        drawer.bottomBar = bottomBar

        toolbar.navView = this
        toolbar.bottomSheet = bottomSheet
        toolbar.toolbarImage = findViewById(R.id.nv_toolbar_image)

        bottomBar.navView = this
        bottomBar.bottomSheet = bottomSheet
        bottomBar.fabView = floatingActionButton
        bottomBar.fabExtendedView = extendedFloatingActionButton

        //bottomSheetBehavior.peekHeight = displayHeight
    }

    fun configSystemBarsUtil(systemBarsUtil: SystemBarsUtil) {
        this.systemBarsUtil = systemBarsUtil.apply {
            this.statusBarBgView = statusBarBackground
            this.navigationBarBgView = navigationBarBackground
            //this.statusBarDarkView = nv_statusBarDarker
            //this.navigationBarDarkView = navigationBarBackground
            //this.insetsListener = nv_drawerLayout
            this.marginBySystemBars = mainView
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
            topMargin =  if (toolbar.enable) {
                toolbar.measure(MATCH_PARENT, WRAP_CONTENT)
                toolbar.measuredHeight
            } else 0
            bottomMargin =  if (bottomBar.enable) {
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
        contentView?.addView(child, index, params)
            ?: super.addView(child, index, params)
}

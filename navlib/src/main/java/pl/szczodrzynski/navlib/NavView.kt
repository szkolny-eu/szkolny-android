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
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import pl.szczodrzynski.navlib.bottomsheet.NavBottomSheet
import pl.szczodrzynski.navlib.databinding.NavViewBinding
import pl.szczodrzynski.navlib.drawer.NavDrawer


class NavView : FrameLayout {
    companion object {
        const val SOURCE_OTHER = 0
        const val SOURCE_DRAWER = 1
        const val SOURCE_BOTTOM_SHEET = 1
    }

    private var contentView: LinearLayout? = null
    private lateinit var statusBarBackground: View
    private lateinit var navigationBarBackground: View
    private lateinit var mainView: LinearLayout
    private lateinit var floatingActionButton: FloatingActionButton
    private lateinit var extendedFloatingActionButton: ExtendedFloatingActionButton

    lateinit var drawer: NavDrawer
    lateinit var toolbar: NavToolbar
    lateinit var bottomBar: NavBottomBar
    lateinit var nightlyText: TextView
    lateinit var bottomSheet: NavBottomSheet

    val coordinator by lazy {
        findViewById<CoordinatorLayout>(R.id.nv_coordinator)
    }

    var navigationLoader: NavigationLoader? = null

    constructor(context: Context) : super(context) {
        create(null, 0)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        create(attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        create(attrs, defStyle)
    }

    private fun create(attrs: AttributeSet?, defStyle: Int) {
        // Load attributes
        val a = context.obtainStyledAttributes(attrs, R.styleable.NavView, defStyle, 0)
        /*_exampleString = a.getString(
            R.styleable.NavView_exampleString
        )*/
        a.recycle()

        val layoutInflater = LayoutInflater.from(context)
        layoutInflater.inflate(R.layout.nav_view, this)
        contentView = findViewById<LinearLayout>(R.id.nv_content)

        statusBarBackground = findViewById(R.id.nv_statusBarBackground)
        navigationBarBackground = findViewById(R.id.nv_navigationBarBackground)
        mainView = findViewById(R.id.nv_main)
        floatingActionButton = findViewById(R.id.nv_floatingActionButton)
        extendedFloatingActionButton = findViewById(R.id.nv_extendedFloatingActionButton)

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

        toolbar.toolbarImage = findViewById(R.id.nv_toolbar_image)
        toolbar.bottomSheet = bottomSheet

        bottomBar.drawer = drawer
        bottomBar.fabView = floatingActionButton
        bottomBar.fabExtendedView = extendedFloatingActionButton

        //bottomSheetBehavior.peekHeight = displayHeight
    }

    private fun convertDpToPixel(dp: Float): Float {
        val resources = context.resources
        val metrics = resources.displayMetrics
        return dp * (metrics.densityDpi / 160f)
    }

    fun configSystemBarsUtil(systemBarsUtil: SystemBarsUtil) {
        this.systemBarsUtil = systemBarsUtil.apply {
            this.statusBarBgView = statusBarBackground
            //this.statusBarDarkView = nv_statusBarDarker
            //this.navigationBarDarkView = navigationBarBackground
            this.marginBySystemBars = mainView
        }
    }

    var enableBottomSheet = true
    var enableBottomSheetDrag = false

    var bottomBarEnable = false

    /**
     * Shows the toolbar and sets the contentView's margin to be
     * below the toolbar.
     */
    var showToolbar = true; set(value) {
        toolbar.visibility = if (value) View.VISIBLE else View.GONE
        field = value
        setContentMargins()
    }

    /**
     * Set the FAB's on click listener
     */
    fun setFabOnClickListener(onClickListener: OnClickListener?) {
        bottomBar.setFabOnClickListener(onClickListener)
    }

    internal var systemBarsUtil: SystemBarsUtil? = null

    private fun setContentMargins() {
        val layoutParams = CoordinatorLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        val actionBarSize = 56 * context.resources.displayMetrics.density
        layoutParams.topMargin = if (showToolbar) actionBarSize.toInt() else 0
        layoutParams.bottomMargin = 0
        contentView?.layoutParams = layoutParams
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)

        Log.d(
            "NavLib",
            "CONFIGURATION CHANGED: ${newConfig?.screenWidthDp}x${newConfig?.screenHeightDp} "+if (newConfig?.orientation == ORIENTATION_PORTRAIT) "portrait" else "landscape"
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

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        if (contentView == null) {
            super.addView(child, index, params)
        }
        else {
            contentView!!.addView(child, index, params)
        }
    }
}

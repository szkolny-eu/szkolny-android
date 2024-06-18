package pl.szczodrzynski.navlib

import android.app.Activity
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.content.res.Resources
import android.graphics.Color
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES
import android.util.Log
import android.view.View
import android.view.View.*
import android.view.Window
import android.view.WindowManager
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import com.mikepenz.materialize.util.KeyboardUtil


class SystemBarsUtil(private val activity: Activity) {
    companion object {
        private const val COLOR_TRANSPARENT = Color.TRANSPARENT
        /**
         * A fallback color.
         * Tells to apply a #22000000 overlay over the status/nav bar color.
         * This has the same effect as [statusBarDarker].
         */
        const val COLOR_HALF_TRANSPARENT = -1
        /**
         * Use ?colorPrimaryDark as a fallback or status bar color.
         */
        const val COLOR_PRIMARY_DARK = -2
        /**
         * A fallback color.
         * Not recommended to use as [statusBarFallbackLight] because it will make status bar
         * icons almost invisible.
         */
        const val COLOR_DO_NOT_CHANGE = -3

        private const val TARGET_MODE_NORMAL = 0
        private const val TARGET_MODE_LIGHT = 1
        private const val TARGET_MODE_GRADIENT = 2
    }

    val window: Window by lazy {
        activity.window
    }
    val resources: Resources by lazy {
        activity.resources
    }

    /**
     * A view which will have the padding added when the soft input keyboard appears.
     */
    var paddingByKeyboard: View? = null
    /**
     * Whether the app should be fullscreen.
     *
     * This means it will display under the system bars
     * and you should probably provide [statusBarBgView],
     * [navigationBarBgView] and [marginBySystemBars].
     */
    var appFullscreen = false

    /**
     * Define the color used to tint the status bar background.
     *
     * Valid values are [COLOR_PRIMARY_DARK] or a color integer.
     *
     * You cannot use neither [COLOR_HALF_TRANSPARENT] nor [COLOR_DO_NOT_CHANGE] here.
     * See [statusBarDarker].
     */
    var statusBarColor = COLOR_PRIMARY_DARK
    /**
     * Whether the status bar should have a dark overlay (#22000000).
     *
     * Useful if the [statusBarColor] is set to a bright color and is the same as an action bar.
     * Not useful if [statusBarColor] is [COLOR_PRIMARY_DARK].
     */
    var statusBarDarker = false
    /**
     * A fallback status bar color used on Android Lollipop
     * when the [statusBarColor] combined with [statusBarDarker] is
     * too bright not to blend with status bar icons (they cannot be
     * set to dark).
     *
     * This will (most likely) not be used when [statusBarDarker] is true.
     *
     * Valid values are [COLOR_HALF_TRANSPARENT], [COLOR_PRIMARY_DARK], [COLOR_DO_NOT_CHANGE].
     */
    var statusBarFallbackLight = COLOR_HALF_TRANSPARENT
    /**
     * A fallback status bar color used on Android KitKat and older.
     * On these systems there is a black-to-transparent gradient as
     * the status bar background.
     *
     * Valid values are [COLOR_HALF_TRANSPARENT], [COLOR_PRIMARY_DARK], [COLOR_DO_NOT_CHANGE].
     */
    var statusBarFallbackGradient = COLOR_DO_NOT_CHANGE

    // TODO remove - test for huawei
    var statusBarTranslucent = false

    /**
     * If false, the nav bar is mostly translucent but not completely transparent.
     */
    var navigationBarTransparent = true

    /**
     * A background view to be resized in order to fit under the status bar.
     */
    var statusBarBgView: View? = null
    /**
     * A background view to be resized in order to fit under the nav bar.
     */
    var navigationBarBgView: View? = null

    /**
     * A dark, half-transparent view to be resized in order to fit under the status bar.
     */
    var statusBarDarkView: View? = null
    /**
     * A dark, half-transparent view to be resized in order to fit under the nav bar.
     */
    var navigationBarDarkView: View? = null

    /**
     * A view which will have the margin added not to overlap with the status/nav bar.
     */
    var marginBySystemBars: View? = null
    /**
     * A view which will listen to the inset applying.
     */
    var insetsListener: View? = null
    /**
     * A view which will have the padding added not to overlap with the nav bar.
     * Useful for persistent bottom sheets.
     * Requires [marginBySystemBars].
     */
    var paddingByNavigationBar: View? = null

    private var keyboardUtil: KeyboardUtil? = null
    private var insetsApplied = false

    fun commit() {
        Log.d("NavLib", "SystemBarsUtil applying")
        insetsApplied = false
        if (paddingByKeyboard != null) {
            // thanks mikepenz for this life-saving class
            keyboardUtil = KeyboardUtil(activity, paddingByKeyboard)
            keyboardUtil?.enable()
        }

        // get the correct target SB color
        var targetStatusBarColor = statusBarColor
        if (targetStatusBarColor == COLOR_PRIMARY_DARK)
            targetStatusBarColor = getColorFromAttr(activity, R.attr.colorPrimaryDark)

        var targetStatusBarDarker = statusBarDarker

        // fallback if the SB color is too light for the icons to be visible
        // applicable on Lollipop 5.0 and TouchWiz 4.1-4.3
        var targetStatusBarFallbackLight = statusBarFallbackLight
        if (targetStatusBarFallbackLight == COLOR_PRIMARY_DARK)
            targetStatusBarFallbackLight = getColorFromAttr(activity, R.attr.colorPrimaryDark)

        // fallback if there is a gradient under the status bar
        // applicable on AOSP/similar 4.4 and Huawei EMUI Lollipop
        // TODO check huawei 6.0+ for gradient bars, check huawei 4.4
        var targetStatusBarFallbackGradient = statusBarFallbackGradient
        if (targetStatusBarFallbackGradient == COLOR_PRIMARY_DARK)
            targetStatusBarFallbackGradient = getColorFromAttr(activity, R.attr.colorPrimaryDark)

        // determines the target mode that will be applied
        var targetStatusBarMode = TARGET_MODE_NORMAL

        val targetStatusBarLight = ColorUtils.calculateLuminance(targetStatusBarColor) > 0.75 && !targetStatusBarDarker

        if (appFullscreen) {
            window.decorView.systemUiVisibility = 0
            // API 19+ (KitKat 4.4+) - make the app fullscreen.
            // On lower APIs this is useless because
            // #1 the status/nav bar cannot be transparent (except Samsung TouchWiz)
            // #2 tablets do not report status/nav bar height correctly
            // #3 SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN makes the activity not resize when keyboard is open
            // Samsung TouchWiz - app will go fullscreen. There is a problem though, see #3.
            var targetAppFullscreen = false
            if (SDK_INT >= VERSION_CODES.KITKAT) {
                targetAppFullscreen = true
            }


            if (SDK_INT in VERSION_CODES.KITKAT until VERSION_CODES.LOLLIPOP) {
                // API 19-20 (KitKat 4.4) - set gradient status bar
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                // take FallbackGradient color
                targetStatusBarMode = TARGET_MODE_GRADIENT
                // disable darker even if [statusBarDarker] == true BUT gradient fallback is not COLOR_HALF_TRANSPARENT
                //targetStatusBarDarker = targetStatusBarDarker && targetStatusBarFallbackGradient == COLOR_HALF_TRANSPARENT
            }
            else if (SDK_INT >= VERSION_CODES.LOLLIPOP) {
                // API 21+ (Lollipop 5.0+) - set transparent status bar
                if (statusBarTranslucent) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                }
                else {
                    window.statusBarColor = Color.TRANSPARENT
                }
                if (SDK_INT < VERSION_CODES.M && targetStatusBarLight) {
                    // take FallbackLight color
                    targetStatusBarMode = TARGET_MODE_LIGHT
                }
            }
            if (SDK_INT >= VERSION_CODES.M && targetStatusBarLight) {
                // API 23+ (Marshmallow 6.0+) - set the status bar icons to dark color if [statusBarLight] is true
                window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
            // FOR SAMSUNG/SONY DEVICES (TouchWiz 4.1-4.3)
            if (SDK_INT < VERSION_CODES.KITKAT) {
                val libs = activity.packageManager.systemSharedLibraryNames
                var reflect: String? = null
                // TODO galaxy s3 - opening keyboard does not resize activity if fullscreen
                if (libs != null) {
                    for (lib in libs) {
                        Log.d("SBU", lib)
                        if (lib == "touchwiz")
                            // SYSTEM_UI_FLAG_TRANSPARENT_BACKGROUND = 0x00001000
                            reflect = "SYSTEM_UI_FLAG_TRANSPARENT_BACKGROUND"
                        else if (lib.startsWith("com.sonyericsson.navigationbar"))
                            reflect = "SYSTEM_UI_FLAG_TRANSPARENT"
                    }
                    if (reflect != null) {
                        try {
                            val field = View::class.java.getField(reflect)
                            var flag = 0
                            if (field.type === Integer.TYPE)
                                flag = field.getInt(null)
                            if (flag != 0) {
                                window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or flag
                                targetStatusBarMode = TARGET_MODE_LIGHT /* or TARGET_MODE_GRADIENT */
                                targetAppFullscreen = true
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
            }
            // TODO huawei detection for 5.0+

            targetStatusBarColor = when (targetStatusBarMode) {
                TARGET_MODE_LIGHT -> when (targetStatusBarFallbackLight) {
                    COLOR_DO_NOT_CHANGE -> targetStatusBarColor
                    COLOR_HALF_TRANSPARENT -> {
                        targetStatusBarDarker = true
                        targetStatusBarColor
                    }
                    else -> targetStatusBarFallbackLight
                }
                TARGET_MODE_GRADIENT -> when (targetStatusBarFallbackGradient) {
                    COLOR_DO_NOT_CHANGE -> {
                        targetStatusBarDarker = false
                        targetStatusBarColor
                    }
                    COLOR_HALF_TRANSPARENT -> {
                        targetStatusBarDarker = true
                        targetStatusBarColor
                    }
                    else -> {
                        targetStatusBarDarker = false
                        targetStatusBarFallbackGradient
                    }
                }
                else -> targetStatusBarColor
            }

            statusBarBgView?.setBackgroundColor(targetStatusBarColor)
            statusBarDarkView?.visibility = if (targetStatusBarDarker) VISIBLE else GONE

            if (targetAppFullscreen) {
                window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            }

            // TODO navigation bar options like status bar
            // NAVIGATION BAR
            if (SDK_INT >= VERSION_CODES.KITKAT && (SDK_INT < VERSION_CODES.LOLLIPOP || !navigationBarTransparent)) {
                // API 19-20 (KitKat 4.4) - set gradient navigation bar
                // API 21+ (Lollipop 5.0+) - set half-transparent navigation bar if [navigationBarTransparent] is false
                window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            }

            if (SDK_INT >= VERSION_CODES.LOLLIPOP && navigationBarTransparent) {
                // API 21+ (Lollipop 5.0+) - set fully transparent navigation bar if [navigationBarTransparent] is true
                window.navigationBarColor = Color.TRANSPARENT
            }

            // PADDING
            if (insetsListener != null) {
                if (SDK_INT >= VERSION_CODES.LOLLIPOP && false) {
                    ViewCompat.setOnApplyWindowInsetsListener(insetsListener!!) { _, insets ->
                        Log.d("NavLib", "Got insets left = ${insets.systemWindowInsetLeft}, top = ${insets.systemWindowInsetTop}, right = ${insets.systemWindowInsetRight}, bottom = ${insets.systemWindowInsetBottom}")
                        if (insetsApplied)
                            return@setOnApplyWindowInsetsListener insets.consumeSystemWindowInsets()
                        Log.d("NavLib", "Applied insets left = ${insets.systemWindowInsetLeft}, top = ${insets.systemWindowInsetTop}, right = ${insets.systemWindowInsetRight}, bottom = ${insets.systemWindowInsetBottom}")
                        insetsApplied = true
                        applyPadding(
                            insets.systemWindowInsetLeft,
                            insets.systemWindowInsetTop,
                            insets.systemWindowInsetRight,
                            insets.systemWindowInsetBottom
                        )
                        insets.consumeSystemWindowInsets()
                    }
                }
                else {
                    var statusBarSize = 0
                    val statusBarRes = resources.getIdentifier("status_bar_height", "dimen", "android")
                    if (statusBarRes > 0 && targetAppFullscreen) {
                        statusBarSize = resources.getDimensionPixelSize(statusBarRes)
                    }

                    applyPadding(
                        0,
                        statusBarSize,
                        0,
                        0
                    )
                }
            }
        }
        else {
            // app not fullscreen
            // TODO statusBarColor & navigationBarColor if not fullscreen (it's possible)
        }
    }

    private fun applyPadding(left: Int, top: Int, right: Int, bottom: Int) {
        marginBySystemBars?.setPadding(left, top, right, bottom)

        statusBarBgView?.layoutParams?.height = top
        navigationBarBgView?.layoutParams?.height = bottom

        statusBarDarkView?.layoutParams?.height = top
        navigationBarDarkView?.layoutParams?.height = bottom

        paddingByNavigationBar?.setPadding(
            (8 * resources.displayMetrics.density).toInt(),
            0,
            (8 * resources.displayMetrics.density).toInt(),
            bottom
        )
    }

    fun destroy() {
        if (paddingByKeyboard != null) {
            keyboardUtil?.disable()
        }
    }
}

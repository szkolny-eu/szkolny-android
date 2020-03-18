/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-22.
 */

package pl.szczodrzynski.edziennik.ui.modules.base

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.ColorUtils
import com.google.android.material.snackbar.Snackbar
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.navlib.getColorFromAttr

class MainSnackbar(val activity: AppCompatActivity) {
    companion object {
        private const val TAG = "MainSnackbar"
    }

    private var snackbar: Snackbar? = null
    private lateinit var coordinator: CoordinatorLayout

    fun setCoordinator(coordinatorLayout: CoordinatorLayout, showAbove: View? = null) {
        this.coordinator = coordinatorLayout
        snackbar = Snackbar.make(coordinator, "", Snackbar.LENGTH_INDEFINITE)
        snackbar?.setAction(R.string.more) {

        }
        val bgColor = ColorUtils.compositeColors(
                getColorFromAttr(activity, R.attr.colorOnSurface) and 0xcfffffff.toInt(),
                getColorFromAttr(activity, R.attr.colorSurface)
        )
        snackbar?.setBackgroundTint(bgColor)
        showAbove?.let { snackbar?.anchorView = it }
    }

    fun snackbar(text: String, actionText: String? = null, onClick: (() -> Unit)? = null) {
        snackbar?.apply {
            setText(text)
            setAction(actionText) {
                onClick?.invoke()
            }
            duration = 7000
            show()
        }
    }

    fun dismiss() = snackbar?.dismiss()
}

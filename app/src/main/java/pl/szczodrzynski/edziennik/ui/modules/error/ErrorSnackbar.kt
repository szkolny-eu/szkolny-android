/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-13.
 */

package pl.szczodrzynski.edziennik.ui.modules.error

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.ColorUtils
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.navlib.getColorFromAttr
import kotlin.coroutines.CoroutineContext

class ErrorSnackbar(val activity: AppCompatActivity) : CoroutineScope {
    companion object {
        private const val TAG = "ErrorSnackbar"
    }

    private var snackbar: Snackbar? = null
    private lateinit var coordinator: CoordinatorLayout
    private val errors = mutableListOf<ApiError>()

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    fun setCoordinator(coordinatorLayout: CoordinatorLayout, showAbove: View? = null) {
        this.coordinator = coordinatorLayout
        snackbar = Snackbar.make(coordinator, R.string.snackbar_error_text, Snackbar.LENGTH_INDEFINITE)
        snackbar?.setAction(R.string.more) {
            ErrorDetailsDialog(activity, errors)
            errors.clear()
        }
        val bgColor = ColorUtils.compositeColors(
                getColorFromAttr(activity, R.attr.colorOnSurface) and 0xcfffffff.toInt(),
                getColorFromAttr(activity, R.attr.colorSurface)
        )
        snackbar?.setBackgroundTint(bgColor)
        showAbove?.let { snackbar?.anchorView = it }
    }

    fun addError(apiError: ApiError): ErrorSnackbar {
        errors += apiError
        snackbar?.setText(apiError.getStringReason(activity))
        return this
    }

    fun show() = snackbar?.show()
    fun dismiss() = snackbar?.dismiss()
}

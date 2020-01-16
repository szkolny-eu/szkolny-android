/*
 * Copyright (c) Kuba Szczodrzyński 2019-11-13.
 */

package pl.szczodrzynski.edziennik.ui.modules.error

import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.ColorUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
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

    private val api by lazy { SzkolnyApi(activity.applicationContext as App) }

    fun setCoordinator(coordinatorLayout: CoordinatorLayout, showAbove: View? = null) {
        this.coordinator = coordinatorLayout
        snackbar = Snackbar.make(coordinator, R.string.snackbar_error_text, Snackbar.LENGTH_INDEFINITE)
        snackbar?.setAction(R.string.more) {
            if (errors.isNotEmpty()) {
                val message = errors.map {
                    listOf(
                            it.getStringReason(activity).asBoldSpannable().asColoredSpannable(R.attr.colorOnBackground.resolveAttr(activity)),
                            if (App.devMode)
                                it.throwable?.stackTraceString ?: it.throwable?.localizedMessage
                            else
                                it.throwable?.localizedMessage
                    ).concat("\n")
                }.concat("\n\n")

                MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.dialog_error_details_title)
                        .setMessage(message)
                        .setPositiveButton(R.string.ok) { dialog, _ ->
                            errors.clear()
                            dialog.dismiss()
                        }
                        .setNeutralButton(R.string.report) { dialog, _ ->
                            launch {
                                val response = withContext(Dispatchers.Default) {
                                    api.errorReport(errors.map { it.toReportableError(activity) })
                                }

                                response?.errors?.ifNotEmpty {
                                    Toast.makeText(activity, "Error: " + it[0].reason, Toast.LENGTH_SHORT).show()
                                    return@launch
                                }
                                errors.clear()
                                dialog.dismiss()
                            }
                        }
                        .show()
            }
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

/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-16.
 */

package pl.szczodrzynski.edziennik.ui.error

import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.*
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.ext.*
import kotlin.coroutines.CoroutineContext

class ErrorDetailsDialog(
        val activity: AppCompatActivity,
        val errors: List<ApiError>,
        val titleRes: Int = R.string.dialog_error_details_title,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {
    companion object {
        private const val TAG = "ApiErrorDialog"
    }

    private lateinit var app: App
    private lateinit var dialog: AlertDialog

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    private val api by lazy { SzkolnyApi(activity.applicationContext as App) }

    init { run {
        if (activity.isFinishing)
            return@run
        onShowListener?.invoke(TAG)
        app = activity.applicationContext as App

        if (errors.isNotEmpty()) {
            val message = errors.map {
                listOf(
                        it.getStringReason(activity).asBoldSpannable().asColoredSpannable(R.attr.colorOnBackground.resolveAttr(activity)),
                        activity.getString(R.string.error_unknown_format, it.errorCode, it.tag),
                        if (App.devMode)
                            it.throwable?.stackTraceString ?: it.throwable?.localizedMessage
                        else
                            it.throwable?.localizedMessage
                ).concat("\n")
            }.concat("\n\n")

            dialog = MaterialAlertDialogBuilder(activity)
                    .setTitle(titleRes)
                    .setMessage(message)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        dialog.dismiss()
                    }
                    .setNeutralButton(R.string.report) { _, _ ->
                        launch {
                            api.runCatching({
                                withContext(Dispatchers.Default) {
                                    errorReport(errors.map { it.toReportableError(activity) })
                                }
                            }, {
                                Toast.makeText(activity, activity.getString(R.string.crash_report_cannot_send) + it, Toast.LENGTH_LONG).show()
                            }) ?: return@launch

                            dialog.dismiss()
                        }
                    }
                    .setCancelable(false)
                    .setOnDismissListener {
                        onDismissListener?.invoke(TAG)
                    }
                    .show()
        }
    }}
}

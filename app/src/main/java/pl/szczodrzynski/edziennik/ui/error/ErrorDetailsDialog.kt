/*
 * Copyright (c) Kuba Szczodrzyński 2020-2-16.
 */

package pl.szczodrzynski.edziennik.ui.error

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.models.ApiError
import pl.szczodrzynski.edziennik.data.api.szkolny.SzkolnyApi
import pl.szczodrzynski.edziennik.ext.*
import pl.szczodrzynski.edziennik.ui.dialogs.base.BaseDialog

class ErrorDetailsDialog(
    activity: AppCompatActivity,
    private val errors: List<ApiError>,
    private val titleRes: Int = R.string.dialog_error_details_title,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BaseDialog(activity, onShowListener, onDismissListener) {

    override val TAG = "ErrorDetailsDialog"

    override fun getTitleRes() = titleRes
    override fun getMessage() = errors.map {
        listOf(
            it.getStringReason(activity)
                .asBoldSpannable()
                .asColoredSpannable(R.attr.colorOnBackground.resolveAttr(activity)),
            activity.getString(R.string.error_unknown_format, it.errorCode, it.tag),
            if (App.devMode)
                it.throwable?.stackTraceString ?: it.throwable?.localizedMessage
            else
                it.throwable?.localizedMessage
        ).concat("\n")
    }.concat("\n\n")

    override fun isCancelable() = false
    override fun getPositiveButtonText() = R.string.close
    override fun getNeutralButtonText() = R.string.report

    override suspend fun onShow() = Unit

    private val api by lazy { SzkolnyApi(activity.applicationContext as App) }

    override suspend fun onBeforeShow(): Boolean {
        return errors.isNotEmpty()
    }

    override suspend fun onNeutralClick(): Boolean {
        api.runCatching({
            withContext(Dispatchers.Default) {
                errorReport(errors.map { it.toReportableError(activity) })
            }
        }, {
            Toast.makeText(
                activity,
                activity.getString(R.string.crash_report_cannot_send) + it,
                Toast.LENGTH_LONG
            ).show()
        })
        return DISMISS
    }
}

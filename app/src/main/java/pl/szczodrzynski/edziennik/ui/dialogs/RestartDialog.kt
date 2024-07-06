/*
 * Copyright (c) Kuba Szczodrzyński 2024-7-5.
 */

package pl.szczodrzynski.edziennik.ui.dialogs

import android.os.Process
import androidx.appcompat.app.AppCompatActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ui.base.dialog.BaseDialog
import kotlin.system.exitProcess

class RestartDialog(
    activity: AppCompatActivity,
    onShowListener: ((tag: String) -> Unit)? = null,
    onDismissListener: ((tag: String) -> Unit)? = null,
) : BaseDialog<Unit>(activity, onShowListener, onDismissListener) {

    override val TAG = "RestartDialog"

    override fun getTitle() = "Restart"
    override fun getMessage() = "Wymagany restart aplikacji"
    override fun isCancelable() = false
    override fun getPositiveButtonText() = R.string.ok

    override suspend fun onPositiveClick(): Boolean {
        Process.killProcess(Process.myPid())
        Runtime.getRuntime().exit(0)
        exitProcess(0)
    }
}

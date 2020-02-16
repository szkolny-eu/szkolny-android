/*
 * Copyright (c) Kuba Szczodrzyński 2020-1-8.
 */

package pl.szczodrzynski.edziennik.ui.dialogs

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.DialogTemplateBinding
import pl.szczodrzynski.edziennik.onClick
import kotlin.coroutines.CoroutineContext

class TemplateDialog(
        val activity: AppCompatActivity,
        val onActionPerformed: (() -> Unit)? = null,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {
    companion object {
        private const val TAG = "TemplateDialog"
    }

    private lateinit var app: App
    private lateinit var b: DialogTemplateBinding
    private lateinit var dialog: AlertDialog

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // local variables go here

    init { run {
        if (activity.isFinishing)
            return@run
        onShowListener?.invoke(TAG)
        app = activity.applicationContext as App
        b = DialogTemplateBinding.inflate(activity.layoutInflater)
        dialog = MaterialAlertDialogBuilder(activity)
                .setView(b.root)
                .setPositiveButton(R.string.close) { dialog, _ ->
                    dialog.dismiss()
                }
                .setNeutralButton(R.string.add, null)
                .setOnDismissListener {
                    onDismissListener?.invoke(TAG)
                }
                .show()

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.onClick {
            // do custom action on neutral button click
            // (does not dismiss the dialog)
        }

        b.clickMe.onClick {
            onActionPerformed?.invoke()
            dialog.dismiss()
        }
    }}
}

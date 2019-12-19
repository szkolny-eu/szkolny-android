/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-19.
 */

package pl.szczodrzynski.edziennik.ui.dialogs

import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.dm7.barcodescanner.zxing.ZXingScannerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.dp
import kotlin.coroutines.CoroutineContext

class QrScannerDialog(
        val activity: AppCompatActivity,
        val onCodeScanned: (text: String) -> Unit,
        val onShowListener: ((tag: String) -> Unit)? = null,
        val onDismissListener: ((tag: String) -> Unit)? = null
) : CoroutineScope {
    companion object {
        private const val TAG = "QrScannerDialog"
    }

    private lateinit var app: App
    private lateinit var scannerView: ZXingScannerView
    private lateinit var dialog: AlertDialog

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    init { run {
        if (activity.isFinishing)
            return@run
        onShowListener?.invoke(TAG)
        app = activity.applicationContext as App
        scannerView = ZXingScannerView(activity)
        scannerView.setPadding(0, 16.dp, 0, 0)
        dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.qr_scanner_dialog_title)
                .setView(scannerView)
                .setPositiveButton(R.string.close) { dialog, _ ->
                    dialog.dismiss()
                }
                .setOnDismissListener {
                    onDismissListener?.invoke(TAG)
                }
                .show()

        scannerView.setResultHandler {
            scannerView.stopCamera()
            dialog.dismiss()
            onCodeScanned(it.text)
        }
        scannerView.startCamera()
    }}
}
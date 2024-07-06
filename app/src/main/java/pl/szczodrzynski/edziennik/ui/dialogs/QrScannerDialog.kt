/*
 * Copyright (c) Kuba Szczodrzyński 2019-12-19.
 */

package pl.szczodrzynski.edziennik.ui.dialogs

import androidx.appcompat.app.AppCompatActivity
import me.dm7.barcodescanner.zxing.ZXingScannerView
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.ext.dp
import pl.szczodrzynski.edziennik.ui.base.dialog.ViewDialog

class QrScannerDialog(
    activity: AppCompatActivity,
    val onCodeScanned: (text: String) -> Unit,
) : ViewDialog<ZXingScannerView>(activity) {

    override fun getTitleRes() = R.string.qr_scanner_dialog_title
    override fun getPositiveButtonText() = R.string.close

    override fun getRootView(): ZXingScannerView {
        val scannerView = ZXingScannerView(activity)
        scannerView.setPadding(0, 16.dp, 2.dp, 0)
        return scannerView
    }

    override suspend fun onShow() {
        root.setResultHandler {
            root.stopCamera()
            dismiss()
            onCodeScanned(it.text)
        }
        root.startCamera()
    }

    override suspend fun onDismiss() {
        root.stopCamera()
    }
}

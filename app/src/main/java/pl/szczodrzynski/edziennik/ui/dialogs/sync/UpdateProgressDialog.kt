/*
 * Copyright (c) Kuba Szczodrzyński 2022-10-22.
 */

package pl.szczodrzynski.edziennik.ui.dialogs.sync

import android.app.DownloadManager
import android.database.CursorIndexOutOfBoundsException
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.szkolny.response.Update
import pl.szczodrzynski.edziennik.databinding.UpdateProgressDialogBinding
import pl.szczodrzynski.edziennik.ext.getInt
import pl.szczodrzynski.edziennik.ext.startCoroutineTimer
import pl.szczodrzynski.edziennik.core.work.UpdateStateEvent
import pl.szczodrzynski.edziennik.ui.base.dialog.BindingDialog
import pl.szczodrzynski.edziennik.utils.Utils

class UpdateProgressDialog(
    activity: AppCompatActivity,
    private val update: Update,
    private val downloadId: Long,
) : BindingDialog<UpdateProgressDialogBinding>(activity) {

    override fun getTitleRes() = R.string.notification_downloading_update
    override fun inflate(layoutInflater: LayoutInflater) =
        UpdateProgressDialogBinding.inflate(layoutInflater)

    override fun isCancelable() = false
    override fun getNegativeButtonText() = R.string.cancel

    override suspend fun onShow() {
        b.update = update
        b.progress.progress = 0

        val downloadManager = app.getSystemService<DownloadManager>() ?: return
        val query = DownloadManager.Query().setFilterById(downloadId)

        startCoroutineTimer(repeatMillis = 100L) {
            try {
                val cursor = downloadManager.query(query)
                cursor.moveToFirst()
                val progress = cursor.getInt(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    ?.toFloat() ?: return@startCoroutineTimer
                b.downloadedSize.text = Utils.readableFileSize(progress.toLong())
                val total = cursor.getInt(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    ?.toFloat() ?: return@startCoroutineTimer
                b.totalSize.text = Utils.readableFileSize(total.toLong())
                b.progress.progress = (progress / total * 100.0f).toInt()
            } catch (_: CursorIndexOutOfBoundsException) {}
        }
    }

    override suspend fun onNegativeClick(): Boolean {
        val downloadManager = app.getSystemService<DownloadManager>() ?: return NO_DISMISS
        downloadManager.remove(downloadId)
        return DISMISS
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onUpdateStateEvent(event: UpdateStateEvent) {
        if (event.downloadId != downloadId)
            return
        EventBus.getDefault().removeStickyEvent(event)
        if (!event.running)
            dismiss()
    }
}

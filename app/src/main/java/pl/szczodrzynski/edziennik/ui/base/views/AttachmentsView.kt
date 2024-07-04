/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-1.
 */

package pl.szczodrzynski.edziennik.ui.base.views

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.AttributeSet
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.api.edziennik.EdziennikTask
import pl.szczodrzynski.edziennik.data.api.events.AttachmentGetEvent
import pl.szczodrzynski.edziennik.ext.isNotNullNorEmpty
import pl.szczodrzynski.edziennik.utils.SimpleDividerItemDecoration
import pl.szczodrzynski.edziennik.utils.Utils
import timber.log.Timber
import java.io.File

class AttachmentsView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {
    companion object {
        private const val TAG = "AttachmentsFragment"
        const val TYPE_MESSAGE = 0
        const val TYPE_EVENT = 1
    }

    private val storageDir by lazy {
        Utils.getStorageDir()
    }

    fun init(arguments: Bundle, owner: Any) {
        val app = context.applicationContext as App
        val activity = context as? AppCompatActivity ?: return
        val list = this as? RecyclerView ?: return

        val profileId = arguments.getInt("profileId")
        val attachmentIds = arguments.getLongArray("attachmentIds") ?: return
        val attachmentNames = arguments.getStringArray("attachmentNames") ?: return
        val attachmentSizes = arguments.getLongArray("attachmentSizes")

        val adapter = AttachmentAdapter(context, onAttachmentClick = { item ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                downloadAttachment(item)
                return@AttachmentAdapter
            }
            app.permissionManager.requestStoragePermission(activity, R.string.permissions_attachment) {
                downloadAttachment(item)
            }
        }, onAttachmentLongClick = { chip, item ->
            val popupMenu = PopupMenu(chip.context, chip)
            popupMenu.menu.add(0, 1, 0, R.string.messages_attachment_download_again)
            popupMenu.setOnMenuItemClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    downloadAttachment(item)
                    return@setOnMenuItemClickListener true
                }
                app.permissionManager.requestStoragePermission(activity, R.string.permissions_attachment) {
                    downloadAttachment(item, forceDownload = true)
                }
                true
            }
            popupMenu.show()
        })

        attachmentIds.forEachIndexed { index, id ->
            val name = attachmentNames[index] ?: return@forEachIndexed
            var size = attachmentSizes?.getOrNull(index)
            // hide the size if less than 1 byte
            if (size?.compareTo(1) == -1)
                size = null

            val item = AttachmentAdapter.Item(profileId, owner, id, name, size)
            adapter.items += item
            checkAttachment(item = item)
        }

        // load & configure the adapter
        if (adapter.items.isNotNullNorEmpty() && list.adapter == null) {
            list.adapter = adapter
            list.apply {
                setHasFixedSize(false)
                isNestedScrollingEnabled = false
                layoutManager = LinearLayoutManager(context)
                addItemDecoration(SimpleDividerItemDecoration(context))
            }
        }
    }

    private fun checkAttachment(item: AttachmentAdapter.Item): Boolean {
        val attachmentDataFile = File(storageDir, "." + item.profileId + "_" + item.ownerId + "_" + item.id)
        item.isDownloaded = if (attachmentDataFile.exists()) {
            try {
                val attachmentFileName = Utils.getStringFromFile(attachmentDataFile)
                val attachmentFile = File(attachmentFileName)
                // get the correct file name and update
                if (attachmentFile.exists()) {

                    // get the download url before updating file name
                    val fileUrl = item.name.substringAfter(":", missingDelimiterValue = "")
                    // update file name with the downloaded one
                    item.name = attachmentFile.name
                    // update file size (useful for items with no defined size)
                    item.size = attachmentFile.length()
                    // save the download url back
                    if (fileUrl != "")
                        item.name += ":$fileUrl"

                    true
                }
                else false
            } catch (e: Exception) {
                Timber.e(e)
                false
            }
        } else false
        return item.isDownloaded
    }

    private fun downloadAttachment(attachment: AttachmentAdapter.Item, forceDownload: Boolean = false) {
        if (!forceDownload && attachment.isDownloaded) {
            // open file by name, or first part before ':' (Vulcan OneDrive)
            Utils.openFile(context, File(Utils.getStorageDir(), attachment.name.substringBefore(":")))
            return
        } else if (attachment.name.contains(":")) {
            Utils.openUrl(context, attachment.name.substringAfter(":"))
        } else {
            attachment.isDownloading = true
            (adapter as? AttachmentAdapter)?.let {
                it.notifyItemChanged(it.items.indexOf(attachment))
            }

            EdziennikTask.attachmentGet(
                attachment.profileId,
                attachment.owner,
                attachment.id,
                attachment.name
            ).enqueue(context)
        }
    }

    private val lastUpdate: Long = 0
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun onAttachmentGetEvent(event: AttachmentGetEvent) {
        EventBus.getDefault().removeStickyEvent(event)
        val attachment = (adapter as? AttachmentAdapter)?.items?.firstOrNull {
            it.profileId == event.profileId
                && it.owner == event.owner
                && it.id == event.attachmentId
        } ?: return


        when (event.eventType) {
            AttachmentGetEvent.TYPE_FINISHED -> {
                // save the downloaded file name
                attachment.isDownloading = false
                attachment.isDownloaded = true

                // get the download url before updating file name
                val fileUrl = attachment.name.substringAfter(":", missingDelimiterValue = "")
                // update file name with the downloaded one
                attachment.name = File(event.fileName ?: return).name
                // save the download url back
                if (fileUrl != "")
                    attachment.name += ":$fileUrl"

                // open the file
                Utils.openFile(context, File(event.fileName))
            }

            AttachmentGetEvent.TYPE_PROGRESS -> {
                attachment.downloadProgress = event.bytesWritten.toFloat() / 1000000f
            }
        }

        if (event.eventType != AttachmentGetEvent.TYPE_PROGRESS || System.currentTimeMillis() - lastUpdate > 100L) {
            (adapter as? AttachmentAdapter)?.let {
                it.notifyItemChanged(it.items.indexOf(attachment))
            }
        }
    }

    override fun onAttachedToWindow() {
        EventBus.getDefault().register(this)
        super.onAttachedToWindow()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        EventBus.getDefault().unregister(this)
    }
}

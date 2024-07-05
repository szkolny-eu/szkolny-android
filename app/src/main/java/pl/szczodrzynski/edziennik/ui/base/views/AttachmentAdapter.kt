/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-1.
 */

package pl.szczodrzynski.edziennik.ui.base.views

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import eu.szkolny.font.SzkolnyFont
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Event
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.databinding.AttachmentListItemBinding
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ext.onLongClick
import pl.szczodrzynski.edziennik.ext.toDrawable
import pl.szczodrzynski.edziennik.utils.Utils
import kotlin.coroutines.CoroutineContext

class AttachmentAdapter(
    val context: Context,
    val onAttachmentClick: (item: Item) -> Unit,
    val onAttachmentLongClick: ((view: Chip, item: Item) -> Unit)? = null
) : RecyclerView.Adapter<AttachmentAdapter.ViewHolder>(), CoroutineScope {
    companion object {
        private const val TAG = "AttachmentAdapter"
    }

    private val app = context.applicationContext as App
    // optional: place the manager here

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    var items = listOf<Item>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = AttachmentListItemBinding.inflate(inflater, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val b = holder.b

        val fileName = item.name.substringBefore(":http")
        // create an icon for the attachment
        val attachmentIcon = when (Utils.getExtensionFromFileName(fileName)) {
            "doc", "docx", "odt", "rtf" -> SzkolnyFont.Icon.szf_file_word_outline
            "xls", "xlsx", "ods" -> SzkolnyFont.Icon.szf_file_excel_outline
            "ppt", "pptx", "odp" -> SzkolnyFont.Icon.szf_file_powerpoint_outline
            "pdf" -> SzkolnyFont.Icon.szf_file_pdf_outline
            "mp3", "wav", "aac" -> SzkolnyFont.Icon.szf_file_music_outline
            "mp4", "avi", "3gp", "mkv", "flv" -> SzkolnyFont.Icon.szf_file_video_outline
            "jpg", "jpeg", "png", "bmp", "gif" -> SzkolnyFont.Icon.szf_file_image_outline
            "zip", "rar", "tar", "7z" -> SzkolnyFont.Icon.szf_zip_box_outline
            "html", "cpp", "c", "h", "css", "java", "py" -> SzkolnyFont.Icon.szf_file_code_outline
            else -> CommunityMaterial.Icon2.cmd_file_document_outline
        }

        b.chip.text = if (item.isDownloading) {
            app.getString(R.string.messages_attachment_downloading_format, fileName, item.downloadProgress)
        }
        else {
            item.size?.let {
                app.getString(R.string.messages_attachment_format, fileName, Utils.readableFileSize(it))
            } ?: fileName
        }

        b.chip.chipIcon = attachmentIcon.toDrawable(context)
        b.chip.closeIcon = CommunityMaterial.Icon.cmd_check.toDrawable(context)

        b.chip.isCloseIconVisible = item.isDownloaded && !item.isDownloading
        // prevent progress bar flickering
        if (b.progressBar.isVisible != item.isDownloading)
            b.progressBar.isVisible = item.isDownloading

        b.chip.onClick { onAttachmentClick(item) }
        onAttachmentLongClick?.let { listener ->
            b.chip.onLongClick { listener(it, item); true }
        }
    }

    override fun getItemCount() = items.size

    data class Item(
            val profileId: Int,
            val owner: Any,
            val id: Long,
            var name: String,
            var size: Long?
    ) {
        val ownerId
            get() = when (owner) {
                is Message -> owner.id
                is Event -> owner.id
                else -> -1
            }
        var isDownloaded = false
        var isDownloading = false
        var downloadProgress: Float = 0f
        var downloadedName: String? = null
    }

    class ViewHolder(val b: AttachmentListItemBinding) : RecyclerView.ViewHolder(b.root)
}

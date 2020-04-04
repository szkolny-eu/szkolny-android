package pl.szczodrzynski.edziennik.ui.modules.messages

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.databinding.MessagesListItemBinding
import pl.szczodrzynski.edziennik.onClick
import pl.szczodrzynski.edziennik.utils.models.Date
import kotlin.coroutines.CoroutineContext

class MessagesAdapter(
        val activity: AppCompatActivity,
        val teachers: List<Teacher>,
        val onItemClick: ((item: MessageFull) -> Unit)? = null
) : RecyclerView.Adapter<MessagesAdapter.ViewHolder>(), CoroutineScope {
    companion object {
        private const val TAG = "TemplateAdapter"
    }

    private val app = activity.applicationContext as App
    // optional: place the manager here

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    var items = listOf<MessageFull>()
    private val typefaceNormal by lazy { Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) }
    private val typefaceBold by lazy { Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = MessagesListItemBinding.inflate(inflater, parent, false)
        return ViewHolder(view)
    }

    @Suppress("DEPRECATION")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val b = holder.b

        item.recipients?.forEach { recipient ->
            if (recipient.fullName == null) {
                recipient.fullName = teachers.firstOrNull { it.id == recipient.id }?.fullName ?: ""
            }
        }

        b.messageSubject.text = item.subject
        b.messageDate.text = Date.fromMillis(item.addedDate).formattedStringShort
        b.messageAttachmentImage.isVisible = item.hasAttachments

        val text = item.body?.take(200) ?: ""
        b.messageBody.text = MessagesUtils.htmlToSpannable(activity, text)

        val isRead = item.type == Message.TYPE_SENT || item.type == Message.TYPE_DRAFT || item.seen
        val typeface = if (isRead) typefaceNormal else typefaceBold
        val style = if (isRead) R.style.NavView_TextView_Small else R.style.NavView_TextView_Normal
        // set text styles
        b.messageSender.setTextAppearance(activity, style)
        b.messageSender.typeface = typeface
        b.messageSubject.setTextAppearance(activity, style)
        b.messageSubject.typeface = typeface
        b.messageDate.setTextAppearance(activity, style)
        b.messageDate.typeface = typeface

        val messageInfo = MessagesUtils.getMessageInfo(app, item, 48, 24, 18, 12)
        b.messageProfileBackground.setImageBitmap(messageInfo.profileImage)
        b.messageSender.text = messageInfo.profileName

        onItemClick?.let { listener ->
            b.root.onClick { listener(item) }
        }
    }

    override fun getItemCount() = items.size

    class ViewHolder(val b: MessagesListItemBinding) : RecyclerView.ViewHolder(b.root)
}

package pl.szczodrzynski.edziennik.ui.modules.messages

import android.graphics.Typeface
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import androidx.core.view.ViewCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.modules.messages.Message
import pl.szczodrzynski.edziennik.data.db.modules.messages.MessageFull
import pl.szczodrzynski.edziennik.databinding.MessagesItemBinding
import pl.szczodrzynski.edziennik.ui.modules.messages.MessagesAdapter.ViewHolder
import pl.szczodrzynski.edziennik.utils.models.Date

class MessagesAdapter(private val app: App, private val onItemClickListener: OnItemClickListener) : Adapter<ViewHolder>() {
    var messageList: List<MessageFull> = ArrayList()
    fun setData(messageList: List<MessageFull>) {
        this.messageList = messageList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return ViewHolder(DataBindingUtil.inflate(inflater, R.layout.messages_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val b = holder.b
        val message = messageList[position]
        b.root.setOnClickListener { v: View? -> onItemClickListener.onItemClick(null, v, position, position.toLong()) }

        ViewCompat.setTransitionName(b.root, message.id.toString())

        b.messageSubject.text = message.subject
        b.messageDate.text = Date.fromMillis(message.addedDate).formattedStringShort
        b.messageAttachmentImage.visibility = if (message.hasAttachments()) View.VISIBLE else View.GONE
        try {
            b.messageBody.text = Html.fromHtml(
                    if (message.body == null) "" else message.body!!
                            .substring(0, message.body!!.length.coerceAtMost(200))
                            .replace("\\[META:[A-z0-9]+;[0-9-]+]".toRegex(), "")
            )
        } catch (e: Exception) {
            // TODO ???
        }

        if (message.type == Message.TYPE_SENT || message.type == Message.TYPE_DRAFT || message.seen) {
            b.messageSender.setTextAppearance(b.messageSender.context, R.style.NavView_TextView_Small)
            b.messageSender.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            b.messageSender.textSize = 16f
            b.messageSubject.setTextAppearance(b.messageSubject.context, R.style.NavView_TextView_Small)
            b.messageSubject.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            b.messageDate.setTextAppearance(b.messageDate.context, R.style.NavView_TextView_Small)
            b.messageDate.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        } else {
            b.messageSender.setTextAppearance(b.messageSender.context, R.style.NavView_TextView_Normal)
            b.messageSender.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            b.messageSender.textSize = 16f
            b.messageSubject.setTextAppearance(b.messageSubject.context, R.style.NavView_TextView_Normal)
            b.messageSubject.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            b.messageDate.setTextAppearance(b.messageDate.context, R.style.NavView_TextView_Normal)
            b.messageDate.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val messageInfo = MessagesUtils.getMessageInfo(app, message, 48, 24, 18, 12)
        b.messageProfileBackground.setImageBitmap(messageInfo.profileImage)
        b.messageSender.text = messageInfo.profileName
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    inner class ViewHolder(var b: MessagesItemBinding) : RecyclerView.ViewHolder(b.root)

}

/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-5.
 */

package pl.szczodrzynski.edziennik.ui.messages.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.databinding.MessagesListItemBinding
import pl.szczodrzynski.edziennik.ext.attachToastHint
import pl.szczodrzynski.edziennik.ext.detachToastHint
import pl.szczodrzynski.edziennik.ext.onClick
import pl.szczodrzynski.edziennik.ext.resolveAttr
import pl.szczodrzynski.edziennik.ui.grades.viewholder.BindableViewHolder
import pl.szczodrzynski.edziennik.ui.messages.MessagesUtils
import pl.szczodrzynski.edziennik.utils.managers.NoteManager
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.navlib.getColorFromAttr

class MessageViewHolder(
    inflater: LayoutInflater,
    parent: ViewGroup,
    val b: MessagesListItemBinding = MessagesListItemBinding.inflate(inflater, parent, false),
) : RecyclerView.ViewHolder(b.root), BindableViewHolder<MessageFull, MessagesAdapter> {
    companion object {
        private const val TAG = "MessageViewHolder"
    }

    override fun onBind(
        activity: AppCompatActivity,
        app: App,
        item: MessageFull,
        position: Int,
        adapter: MessagesAdapter,
    ) {
        b.messageDate.text = Date.fromMillis(item.addedDate).formattedStringShort
        b.messageAttachmentImage.isVisible = item.hasAttachments

        b.messageBody.text = item.bodyHtml?.take(200)

        val isRead = item.isSent || item.isDraft || item.seen
        val typeface = if (isRead) adapter.typefaceNormal else adapter.typefaceBold
        val textColor = if (isRead) getColorFromAttr(b.root.context, R.attr.colorOnSurfaceVariant) else getColorFromAttr(b.root.context, R.attr.colorOnSurface)
        // set text styles
        b.messageSender.setTextColor(textColor)
        b.messageSender.typeface = typeface
        b.messageSubject.setTextColor(textColor)
        b.messageSubject.typeface = typeface
        b.messageDate.setTextColor(textColor)
        b.messageDate.typeface = typeface

        if (adapter.onStarClick == null) {
            b.messageStar.isVisible = false
        }
        b.messageStar.detachToastHint()

        val messageInfo = MessagesUtils.getMessageInfo(app, item, 48, 24, 18, 12)
        b.messageProfileBackground.setImageBitmap(messageInfo.profileImage)

        val colorHighlight = R.attr.colorControlHighlight.resolveAttr(activity)
        b.messageSubject.text = adapter.highlightSearchText(
            item = item,
            text = item.subject,
            color = colorHighlight
        )
        b.messageSender.text = adapter.highlightSearchText(
            item = item,
            text = messageInfo.profileName ?: "",
            color = colorHighlight
        )

        if (adapter.showNotes)
            NoteManager.prependIcon(item, b.messageSubject)

        adapter.onMessageClick?.let { listener ->
            b.root.onClick { listener(item) }
        }
        adapter.onStarClick?.let { listener ->
            b.messageStar.isVisible = true
            adapter.manager.setStarIcon(b.messageStar, item)
            b.messageStar.onClick { listener(item) }
            b.messageStar.attachToastHint(R.string.hint_message_star)
        }
    }
}

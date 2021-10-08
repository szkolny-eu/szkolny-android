/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-5.
 */

package pl.szczodrzynski.edziennik.ui.modules.messages.viewholder

import android.graphics.Typeface
import android.text.style.BackgroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.databinding.MessagesListItemBinding
import pl.szczodrzynski.edziennik.ui.modules.grades.viewholder.BindableViewHolder
import pl.szczodrzynski.edziennik.ui.modules.messages.MessagesAdapter
import pl.szczodrzynski.edziennik.ui.modules.messages.MessagesUtils
import pl.szczodrzynski.edziennik.utils.models.Date

class MessageViewHolder(
    inflater: LayoutInflater,
    parent: ViewGroup,
    val b: MessagesListItemBinding = MessagesListItemBinding.inflate(inflater, parent, false)
) : RecyclerView.ViewHolder(b.root), BindableViewHolder<MessageFull, MessagesAdapter> {
    companion object {
        private const val TAG = "MessageViewHolder"
    }

    override fun onBind(
        activity: AppCompatActivity,
        app: App,
        item: MessageFull,
        position: Int,
        adapter: MessagesAdapter
    ) {
        b.messageSubject.text = item.subject
        b.messageDate.text = Date.fromMillis(item.addedDate).formattedStringShort
        b.messageAttachmentImage.isVisible = item.hasAttachments

        val text = item.body?.take(200) ?: ""
        b.messageBody.text = MessagesUtils.htmlToSpannable(activity, text)

        val isRead = item.type == Message.TYPE_SENT || item.type == Message.TYPE_DRAFT || item.seen
        val typeface = if (isRead) adapter.typefaceNormal else adapter.typefaceBold
        val style = if (isRead) R.style.NavView_TextView_Small else R.style.NavView_TextView_Normal
        // set text styles
        b.messageSender.setTextAppearance(activity, style)
        b.messageSender.typeface = typeface
        b.messageSubject.setTextAppearance(activity, style)
        b.messageSubject.typeface = typeface
        b.messageDate.setTextAppearance(activity, style)
        b.messageDate.typeface = typeface

        if (adapter.onStarClick == null) {
            b.messageStar.isVisible = false
        }
        b.messageStar.detachToastHint()

        val messageInfo = MessagesUtils.getMessageInfo(app, item, 48, 24, 18, 12)
        b.messageProfileBackground.setImageBitmap(messageInfo.profileImage)
        b.messageSender.text = messageInfo.profileName

        item.searchHighlightText?.toString()?.let { highlight ->
            val colorHighlight = R.attr.colorControlHighlight.resolveAttr(activity)

            b.messageSubject.text = b.messageSubject.text.asSpannable(
                StyleSpan(Typeface.BOLD), BackgroundColorSpan(colorHighlight),
                substring = highlight, ignoreCase = true, ignoreDiacritics = true
            )
            b.messageSender.text = b.messageSender.text.asSpannable(
                StyleSpan(Typeface.BOLD), BackgroundColorSpan(colorHighlight),
                substring = highlight, ignoreCase = true, ignoreDiacritics = true
            )
        }

        adapter.onItemClick?.let { listener ->
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

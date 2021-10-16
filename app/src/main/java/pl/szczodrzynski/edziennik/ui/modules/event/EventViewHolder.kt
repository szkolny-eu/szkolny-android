/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-10.
 */

package pl.szczodrzynski.edziennik.ui.modules.event

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.databinding.EventListItemBinding
import pl.szczodrzynski.edziennik.ui.modules.grades.viewholder.BindableViewHolder
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Week

class EventViewHolder(
    inflater: LayoutInflater,
    parent: ViewGroup,
    val b: EventListItemBinding = EventListItemBinding.inflate(inflater, parent, false),
) : RecyclerView.ViewHolder(b.root), BindableViewHolder<EventFull, EventListAdapter> {
    companion object {
        private const val TAG = "EventViewHolder"
    }

    override fun onBind(
        activity: AppCompatActivity,
        app: App,
        item: EventFull,
        position: Int,
        adapter: EventListAdapter,
    ) {
        val manager = app.eventManager

        b.root.onClick {
            adapter.onItemClick?.invoke(item)
            if (!item.seen) {
                manager.markAsSeen(item)
            }
            if (item.showAsUnseen == true) {
                item.showAsUnseen = false
                adapter.notifyItemChanged(item)
            }
        }

        val bullet = " • "
        val colorHighlight = R.attr.colorControlHighlight.resolveAttr(activity)

        b.simpleMode = adapter.simpleMode

        manager.setEventTopic(b.topic, item, showType = false)
        b.topic.text = adapter.highlightSearchText(
            item = item,
            text = b.topic.text,
            color = colorHighlight
        )
        b.topic.maxLines = if (adapter.simpleMode) 2 else 3

        b.details.text = mutableListOf(
            if (adapter.showWeekDay)
                Week.getFullDayName(item.date.weekDay)
            else null,
            if (adapter.showDate)
                item.date.getRelativeString(activity, 7) ?: item.date.formattedStringShort
            else null,
            if (adapter.showType)
                item.typeName
            else null,
            if (adapter.showTime)
                item.time?.stringHM ?: app.getString(R.string.event_all_day)
            else null,
            if (adapter.showSubject)
                adapter.highlightSearchText(
                    item = item,
                    text = item.subjectLongName ?: "",
                    color = colorHighlight
                )
            else null,
        ).concat(bullet)

        val addedBy = item.sharedByName ?: item.teacherName ?: ""
        b.addedBy.setText(
            when (item.sharedBy) {
                null -> when {
                    item.addedManually -> R.string.event_list_added_by_self_format
                    item.teacherName == null -> R.string.event_list_added_by_unknown_format
                    else -> R.string.event_list_added_by_format
                }
                "self" -> R.string.event_list_shared_by_self_format
                else -> R.string.event_list_shared_by_format
            },
            /* 1$ */
            Date.fromMillis(item.addedDate).formattedString,
            /* 2$ */
            addedBy,
            /* 3$ */
            item.teamName?.let { bullet + it } ?: "",
        )
        // workaround for the span data lost during setText above
        val addedBySpanned = adapter.highlightSearchText(
            item = item,
            text = addedBy,
            color = colorHighlight
        )
        b.addedBy.text = b.addedBy.text.replaceSpanned(addedBy, addedBySpanned)

        b.attachmentIcon.isVisible = item.hasAttachments

        b.typeColor.background?.setTintColor(item.eventColor)
        b.typeColor.isVisible = adapter.showType

        b.editButton.isVisible = !adapter.simpleMode && item.addedManually && !item.isDone
        b.editButton.onClick {
            adapter.onEventEditClick?.invoke(item)
        }
        b.editButton.attachToastHint(R.string.hint_edit_event)

        if (item.showAsUnseen == null)
            item.showAsUnseen = !item.seen

        b.unread.isVisible = item.showAsUnseen == true
        if (adapter.markAsSeen && !item.seen) {
            manager.markAsSeen(item)
        }
    }
}

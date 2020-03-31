/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-30
 */

package pl.szczodrzynski.edziennik.ui.dialogs.event

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.databinding.EventListItemBinding
import pl.szczodrzynski.edziennik.utils.models.Date
import pl.szczodrzynski.edziennik.utils.models.Week

class EventListAdapter(
        val context: Context,
        val simpleMode: Boolean = false,
        val showWeekDay: Boolean = false,
        val showDate: Boolean = false,
        val showType: Boolean = true,
        val showTime: Boolean = true,
        val showSubject: Boolean = true,
        val onItemClick: ((event: EventFull) -> Unit)? = null,
        val onEventEditClick: ((event: EventFull) -> Unit)? = null
) : RecyclerView.Adapter<EventListAdapter.ViewHolder>() {

    private val app by lazy { context.applicationContext as App }

    var items = listOf<EventFull>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = EventListItemBinding.inflate(inflater, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = items[position]
        val b = holder.b

        b.root.onClick {
            onItemClick?.invoke(event)
        }

        val bullet = " • "

        b.simpleMode = simpleMode

        b.topic.text = event.topic
        b.topic.maxLines = if (simpleMode) 2 else 3

        b.details.text = mutableListOf<CharSequence?>(
                if (showWeekDay) Week.getFullDayName(event.date.weekDay) else null,
                if (showDate) event.date.getRelativeString(context, 7) ?: event.date.formattedStringShort else null,
                if (showType) event.typeName else null,
                if (showTime) event.time?.stringHM ?: app.getString(R.string.event_all_day) else null,
                if (showSubject) event.subjectLongName else null
        ).concat(bullet)

        b.addedBy.setText(
                when (event.sharedBy) {
                    null -> when {
                        event.addedManually -> R.string.event_list_added_by_self_format
                        event.teacherName == null -> R.string.event_list_added_by_unknown_format
                        else -> R.string.event_list_added_by_format
                    }
                    "self" -> R.string.event_list_shared_by_self_format
                    else -> R.string.event_list_shared_by_format
                },
                Date.fromMillis(event.addedDate).formattedString,
                event.sharedByName ?: event.teacherName ?: "",
                event.teamName?.let { bullet+it } ?: ""
        )

        b.typeColor.background?.setTintColor(event.eventColor)
        b.typeColor.isVisible = showType

        b.editButton.isVisible = !simpleMode && event.addedManually && !event.isDone
        b.editButton.onClick {
            onEventEditClick?.invoke(event)
        }
        b.editButton.attachToastHint(R.string.hint_edit_event)

        b.isDone.isVisible = event.isDone
    }

    override fun getItemCount() = items.size

    class ViewHolder(val b: EventListItemBinding) : RecyclerView.ViewHolder(b.root)
}

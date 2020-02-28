/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-30
 */

package pl.szczodrzynski.edziennik.ui.dialogs.event

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.*
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.databinding.EventListItemBinding
import pl.szczodrzynski.edziennik.utils.models.Date

class EventListAdapter(
        val context: Context,
        val simpleMode: Boolean = false,
        val showDate: Boolean = false,
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

        b.details.text = mutableListOf<CharSequence?>(
                if (showDate) event.eventDate.getRelativeString(context, 7) ?: event.eventDate.formattedStringShort else null,
                event.typeName,
                if (simpleMode) null else event.startTime?.stringHM ?: app.getString(R.string.event_all_day),
                if (simpleMode) null else event.subjectLongName
        ).concat(bullet)

        b.addedBy.setText(
                when (event.sharedBy) {
                    null -> when {
                        event.addedManually -> R.string.event_list_added_by_self_format
                        event.teacherFullName == null -> R.string.event_list_added_by_unknown_format
                        else -> R.string.event_list_added_by_format
                    }
                    "self" -> R.string.event_list_shared_by_self_format
                    else -> R.string.event_list_shared_by_format
                },
                Date.fromMillis(event.addedDate).formattedString,
                event.sharedByName ?: event.teacherFullName ?: "",
                event.teamName?.let { bullet+it } ?: ""
        )

        b.typeColor.background?.setTintColor(event.getColor())

        b.editButton.visibility = if (event.addedManually) View.VISIBLE else View.GONE
        b.editButton.onClick {
            onEventEditClick?.invoke(event)
        }

        /*with(holder) {
            b.eventListItemRoot.background.colorFilter = when (event.type) {
                Event.TYPE_HOMEWORK -> PorterDuffColorFilter(0xffffffff.toInt(), PorterDuff.Mode.CLEAR)
                else -> PorterDuffColorFilter(event.color, PorterDuff.Mode.MULTIPLY)
            }

            b.eventListItemStartTime.text = if (event.startTime == null) app.getString(R.string.event_all_day) else event.startTime?.stringHM
            b.eventListItemTeamName.text = bs(event.teamName)
            b.eventListItemTeacherName.text = app.getString(R.string.concat_2_strings, bs(null, event.teacherFullName, "\n"), bs(event.subjectLongName))
            b.eventListItemAddedDate.text = Date.fromMillis(event.addedDate).formattedStringShort
            b.eventListItemType.text = event.typeName
            b.eventListItemTopic.text = event.topic
            b.eventListItemHomework.visibility = if (event.type == Event.TYPE_HOMEWORK) View.VISIBLE else View.GONE
            b.eventListItemSharedBy.text = app.getString(R.string.event_shared_by_format, if (event.sharedBy == "self") app.getString(R.string.event_shared_by_self) else event.sharedByName)
            b.eventListItemSharedBy.visibility = if (event.sharedByName.isNullOrBlank()) View.GONE else View.VISIBLE

            b.eventListItemEdit.visibility = if (event.addedManually) View.VISIBLE else View.GONE
            b.eventListItemEdit.setOnClickListener {
                parentDialog.dismiss()

                EventManualDialog(
                        context as MainActivity,
                        event.profileId,
                        editingEvent = event,
                        onShowListener = parentDialog.onShowListener,
                        onDismissListener = parentDialog.onDismissListener
                )
            }
        }*/
    }

    override fun getItemCount() = items.size

    class ViewHolder(val b: EventListItemBinding) : RecyclerView.ViewHolder(b.root)
}

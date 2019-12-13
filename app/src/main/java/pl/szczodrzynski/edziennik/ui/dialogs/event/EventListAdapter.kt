/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-30
 */

package pl.szczodrzynski.edziennik.ui.dialogs.event

import android.content.Context
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.MainActivity
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.data.db.modules.events.Event
import pl.szczodrzynski.edziennik.data.db.modules.events.EventFull
import pl.szczodrzynski.edziennik.databinding.RowDialogEventListItemBinding
import pl.szczodrzynski.edziennik.utils.Utils.bs
import pl.szczodrzynski.edziennik.utils.models.Date

class EventListAdapter(
        val context: Context,
        val parentDialog: EventListDialog
) : RecyclerView.Adapter<EventListAdapter.ViewHolder>() {

    private val app by lazy { context.applicationContext as App }

    val eventList = mutableListOf<EventFull>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view: RowDialogEventListItemBinding = DataBindingUtil.inflate(inflater, R.layout.row_dialog_event_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val event = eventList[position]

        holder.apply {
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
        }
    }

    override fun getItemCount(): Int = eventList.size

    class ViewHolder(val b: RowDialogEventListItemBinding) : RecyclerView.ViewHolder(b.root)
}

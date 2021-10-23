/*
 * Copyright (c) Kacper Ziubryniewicz 2019-11-30
 */

package pl.szczodrzynski.edziennik.ui.event

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.full.EventFull
import pl.szczodrzynski.edziennik.ext.startCoroutineTimer
import pl.szczodrzynski.edziennik.ui.search.SearchableAdapter
import kotlin.coroutines.CoroutineContext

class EventListAdapter(
    val activity: AppCompatActivity,
    val simpleMode: Boolean = false,
    val showWeekDay: Boolean = false,
    val showDate: Boolean = false,
    val showType: Boolean = true,
    val showTime: Boolean = true,
    val showSubject: Boolean = true,
    val markAsSeen: Boolean = true,
    isReversed: Boolean = false,
    val onEventClick: ((event: EventFull) -> Unit)? = null,
    val onEventEditClick: ((event: EventFull) -> Unit)? = null,
) : SearchableAdapter<EventFull>(isReversed), CoroutineScope {
    companion object {
        private const val TAG = "EventListAdapter"
        private const val ITEM_TYPE_EVENT = 0
    }

    private val app = activity.applicationContext as App
    private val manager
        get() = app.eventManager

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    override fun getItemViewType(item: EventFull) = ITEM_TYPE_EVENT

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        item: EventFull,
    ) {
        if (holder !is EventViewHolder)
            return
        holder.onBind(activity, app, item, position, this)
    }

    override fun onCreateViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int,
    ) = EventViewHolder(inflater, parent)

    internal fun notifyItemChanged(model: Any) {
        startCoroutineTimer(1000L, 0L) {
            val index = items.indexOf(model)
            if (index != -1)
                notifyItemChanged(index)
        }
    }
}

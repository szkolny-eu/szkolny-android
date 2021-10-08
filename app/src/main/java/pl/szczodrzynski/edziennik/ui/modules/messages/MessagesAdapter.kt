package pl.szczodrzynski.edziennik.ui.modules.messages

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filterable
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.ui.modules.grades.viewholder.BindableViewHolder
import pl.szczodrzynski.edziennik.ui.modules.messages.models.MessagesSearch
import pl.szczodrzynski.edziennik.ui.modules.messages.utils.MessagesFilter
import pl.szczodrzynski.edziennik.ui.modules.messages.viewholder.MessageViewHolder
import pl.szczodrzynski.edziennik.ui.modules.messages.viewholder.SearchViewHolder
import kotlin.coroutines.CoroutineContext

class MessagesAdapter(
    val activity: AppCompatActivity,
    val teachers: List<Teacher>,
    val onItemClick: ((item: MessageFull) -> Unit)? = null,
    val onStarClick: ((item: MessageFull) -> Unit)? = null,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), CoroutineScope, Filterable {
    companion object {
        private const val TAG = "MessagesAdapter"
        private const val ITEM_TYPE_MESSAGE = 0
        private const val ITEM_TYPE_SEARCH = 1
    }

    private val app = activity.applicationContext as App
    // optional: place the manager here
    internal val manager
        get() = app.messageManager

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    // mutable var changed by the filter
    var items = listOf<Any>()
    // mutable list managed by the fragment
    val allItems = mutableListOf<Any>()
    val typefaceNormal: Typeface by lazy { Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) }
    val typefaceBold: Typeface by lazy { Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ITEM_TYPE_MESSAGE -> MessageViewHolder(inflater, parent)
            ITEM_TYPE_SEARCH -> SearchViewHolder(inflater, parent)
            else -> throw IllegalArgumentException("Incorrect viewType")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is MessageFull -> ITEM_TYPE_MESSAGE
            is MessagesSearch -> ITEM_TYPE_SEARCH
            else -> throw IllegalArgumentException("Incorrect viewType")
        }
    }

    @Suppress("DEPRECATION")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder !is BindableViewHolder<*, *>)
            return

        when {
            holder is MessageViewHolder
                    && item is MessageFull -> holder.onBind(activity, app, item, position, this)
            holder is SearchViewHolder
                    && item is MessagesSearch -> holder.onBind(activity, app, item, position, this)
        }
    }

    private val messagesFilter by lazy {
        MessagesFilter(this)
    }
    override fun getItemCount() = items.size
    override fun getFilter() = messagesFilter
}

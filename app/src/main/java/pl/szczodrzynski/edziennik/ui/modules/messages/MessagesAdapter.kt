package pl.szczodrzynski.edziennik.ui.modules.messages

import android.graphics.Typeface
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.cleanDiacritics
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.entity.Teacher
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.ui.modules.grades.viewholder.BindableViewHolder
import pl.szczodrzynski.edziennik.ui.modules.messages.models.MessagesSearch
import pl.szczodrzynski.edziennik.ui.modules.messages.viewholder.MessageViewHolder
import pl.szczodrzynski.edziennik.ui.modules.messages.viewholder.SearchViewHolder
import java.util.*
import kotlin.coroutines.CoroutineContext
import kotlin.math.min

class MessagesAdapter(
        val activity: AppCompatActivity,
        val teachers: List<Teacher>,
        val onItemClick: ((item: MessageFull) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>(), CoroutineScope, Filterable {
    companion object {
        private const val TAG = "MessagesAdapter"
        private const val ITEM_TYPE_MESSAGE = 0
        private const val ITEM_TYPE_SEARCH = 1
    }

    private val app = activity.applicationContext as App
    // optional: place the manager here

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main

    var items = mutableListOf<Any>()
    var allItems = mutableListOf<Any>()
    val typefaceNormal: Typeface by lazy { Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) }
    val typefaceBold: Typeface by lazy { Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
    private val comparator by lazy { Comparator { o1: Any, o2: Any ->
        if (o1 !is MessageFull || o2 !is MessageFull)
            return@Comparator 0
        when {
            // standard sorting
            o1.filterWeight > o2.filterWeight -> return@Comparator 1
            o1.filterWeight < o2.filterWeight -> return@Comparator -1
            else -> when {
                // reversed sorting
                o1.addedDate > o2.addedDate -> return@Comparator -1
                o1.addedDate < o2.addedDate -> return@Comparator 1
                else -> return@Comparator 0
            }
        }
    }}

    val textWatcher by lazy {
        object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                getFilter().filter(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                /*items.getOrNull(0)?.let {
                    if (it is MessagesSearch) {
                        it.searchText = s?.toString() ?: ""
                    }
                }*/
            }
        }
    }

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
            holder is MessageViewHolder && item is MessageFull -> holder.onBind(activity, app, item, position, this)
            holder is SearchViewHolder && item is MessagesSearch -> holder.onBind(activity, app, item, position, this)
        }
    }

    override fun getItemCount() = items.size
    override fun getFilter() = filter
    private var prevCount = -1
    private val filter by lazy { object : Filter() {
        override fun performFiltering(prefix: CharSequence?): FilterResults {
            val results = FilterResults()

            if (prevCount == -1)
                prevCount = allItems.size

            if (prefix.isNullOrEmpty()) {
                allItems.forEach {
                    if (it is MessageFull)
                        it.searchHighlightText = null
                }
                results.values = allItems.toList()
                results.count = allItems.size
                return results
            }

            val items = mutableListOf<Any>()
            val prefixString = prefix.toString()

            allItems.forEach {
                if (it !is MessageFull) {
                    items.add(it)
                    return@forEach
                }
                it.filterWeight = 100
                it.searchHighlightText = null

                var weight: Int
                if (it.type == Message.TYPE_SENT) {
                    it.recipients?.forEach { recipient ->
                        weight = getMatchWeight(recipient.fullName, prefixString)
                        if (weight != 100) {
                            if (weight == 3)
                                weight = 31
                            it.filterWeight = min(it.filterWeight, 10 + weight)
                        }
                    }
                }
                else {
                    weight = getMatchWeight(it.senderName, prefixString)
                    if (weight != 100) {
                        if (weight == 3)
                            weight = 31
                        it.filterWeight = min(it.filterWeight, 10 + weight)
                    }
                }


                weight = getMatchWeight(it.subject, prefixString)
                if (weight != 100) {
                    if (weight == 3)
                        weight = 22
                    it.filterWeight = min(it.filterWeight, 20 + weight)
                }

                if (it.filterWeight != 100) {
                    it.searchHighlightText = prefixString
                    items.add(it)
                }
            }

            Collections.sort(items, comparator)
            results.values = items
            results.count = items.size
            return results
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            results.values?.let { items = it as MutableList<Any> }
            // do not re-bind the search box
            val count = results.count - 1

            // this tries to update every item except the search field
            when {
                count > prevCount -> {
                    notifyItemRangeInserted(prevCount + 1, count - prevCount)
                    notifyItemRangeChanged(1, prevCount)
                }
                count < prevCount -> {
                    notifyItemRangeRemoved(prevCount + 1, prevCount - count)
                    notifyItemRangeChanged(1, count)
                }
                else -> {
                    notifyItemRangeChanged(1, count)
                }
            }

            /*if (prevCount != count) {
                items.getOrNull(0)?.let {
                    if (it is MessagesSearch) {
                        it.count = count
                        notifyItemChanged(0)
                    }
                }
            }*/

            prevCount = count
        }
    }}

    private fun getMatchWeight(name: CharSequence?, prefix: String): Int {
        if (name == null)
            return 100

        val nameClean = name.cleanDiacritics()

        // First match against the whole, non-split value
        if (nameClean.startsWith(prefix, ignoreCase = true) || name.startsWith(prefix, ignoreCase = true)) {
            return 1
        } else {
            // check if prefix matches any of the words
            val words = nameClean.split(" ").toTypedArray() + name.split(" ").toTypedArray()
            for (word in words) {
                if (word.startsWith(prefix, ignoreCase = true)) {
                    return 2
                }
            }
        }
        // finally check if the prefix matches any part of the name
        if (nameClean.contains(prefix, ignoreCase = true) || name.contains(prefix, ignoreCase = true)) {
            return 3
        }

        return 100
    }
}

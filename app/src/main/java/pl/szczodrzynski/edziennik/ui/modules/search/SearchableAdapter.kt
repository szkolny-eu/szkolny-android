/*
 * Copyright (c) Kuba Szczodrzyński 2021-10-10.
 */

package pl.szczodrzynski.edziennik.ui.modules.search

import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filterable
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.asSpannable
import pl.szczodrzynski.edziennik.utils.span.BoldSpan

abstract class SearchableAdapter<T : Searchable<T>> :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(), Filterable {
    companion object {
        const val ITEM_TYPE_SEARCH = 2137
    }

    // mutable var changed by the filter
    val allItems = mutableListOf<T>()
    // mutable list managed by the fragment
    var items = listOf<T>()
        internal set

    fun setAllItems(items: List<T>, searchText: String? = null, addSearchField: Boolean = true) {
        if (allItems.isEmpty()) {
            // items empty - add the search field
            if (addSearchField) {
                @Suppress("UNCHECKED_CAST") // what ???
                allItems += SearchField(searchText ?: "") as T
            }
        } else {
            // items not empty - remove all except the search field
            allItems.removeAll { it !is SearchField }
        }
        // add all new items
        allItems.addAll(items)
    }

    /**
     * Return the search field in this adapter's list, or null if not found.
     */
    fun getSearchField(): SearchField? {
        return allItems.filterIsInstance<SearchField>().firstOrNull()
    }

    fun highlightSearchText(item: T, text: CharSequence, color: Int): CharSequence {
        if (item.searchHighlightText == null)
            return text
        return text.asSpannable(
            BoldSpan(),
            BackgroundColorSpan(color),
            substring = item.searchHighlightText,
            ignoreCase = true,
            ignoreDiacritics = true,
        )
    }

    final override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            ITEM_TYPE_SEARCH -> SearchViewHolder(inflater, parent)
            else -> onCreateViewHolder(inflater, parent, viewType)
        }
    }

    final override fun getItemViewType(position: Int): Int {
        return when (val item = items[position]) {
            is SearchField -> ITEM_TYPE_SEARCH
            else -> getItemViewType(item)
        }
    }

    final override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        if (holder is SearchViewHolder && item is SearchField) {
            holder.bind(item, this)
        } else {
            onBindViewHolder(holder, position, item)
        }
    }

    abstract fun getItemViewType(item: T): Int
    abstract fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, item: T)
    abstract fun onCreateViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder

    private val filter = SearchFilter(this)
    override fun getItemCount() = items.size
    override fun getFilter() = filter
}

/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-5.
 */

package pl.szczodrzynski.edziennik.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.SearchItemBinding

class SearchViewHolder(
    inflater: LayoutInflater,
    parent: ViewGroup,
    val b: SearchItemBinding = SearchItemBinding.inflate(
        inflater,
        parent,
        false
    ),
) : RecyclerView.ViewHolder(b.root) {
    companion object {
        private const val TAG = "SearchViewHolder"
    }

    internal fun bind(item: SearchField, adapter: SearchableAdapter<*>) {
        val watcher = SearchTextWatcher(b, adapter.filter, item)
        b.searchEdit.removeTextChangedListener(watcher)

        if (adapter.items.isEmpty() || adapter.items.size == adapter.allItems.size)
            b.searchLayout.helperText = " "
        else
            b.searchLayout.helperText =
                b.root.context.getString(R.string.messages_search_results, adapter.items.size - 1)
        b.searchEdit.setText(item.searchText)

        b.searchEdit.addTextChangedListener(watcher)
    }
}

/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-5.
 */

package pl.szczodrzynski.edziennik.ui.modules.messages.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.MessagesListItemSearchBinding
import pl.szczodrzynski.edziennik.ui.modules.grades.viewholder.BindableViewHolder
import pl.szczodrzynski.edziennik.ui.modules.messages.MessagesAdapter
import pl.szczodrzynski.edziennik.ui.modules.messages.models.MessagesSearch
import pl.szczodrzynski.edziennik.ui.modules.messages.utils.SearchTextWatcher

class SearchViewHolder(
    inflater: LayoutInflater,
    parent: ViewGroup,
    val b: MessagesListItemSearchBinding = MessagesListItemSearchBinding.inflate(
        inflater,
        parent,
        false
    )
) : RecyclerView.ViewHolder(b.root), BindableViewHolder<MessagesSearch, MessagesAdapter> {
    companion object {
        private const val TAG = "SearchViewHolder"
    }

    override fun onBind(
        activity: AppCompatActivity,
        app: App,
        item: MessagesSearch,
        position: Int,
        adapter: MessagesAdapter
    ) {
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

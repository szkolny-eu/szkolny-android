/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-5.
 */

package pl.szczodrzynski.edziennik.ui.modules.messages.viewholder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import pl.szczodrzynski.edziennik.App
import pl.szczodrzynski.edziennik.databinding.MessagesListItemSearchBinding
import pl.szczodrzynski.edziennik.ui.modules.grades.viewholder.BindableViewHolder
import pl.szczodrzynski.edziennik.ui.modules.messages.MessagesAdapter
import pl.szczodrzynski.edziennik.ui.modules.messages.models.MessagesSearch

class SearchViewHolder(
        inflater: LayoutInflater,
        parent: ViewGroup,
        val b: MessagesListItemSearchBinding = MessagesListItemSearchBinding.inflate(inflater, parent, false)
) : RecyclerView.ViewHolder(b.root), BindableViewHolder<MessagesSearch, MessagesAdapter> {
    companion object {
        private const val TAG = "SearchViewHolder"
    }

    override fun onBind(activity: AppCompatActivity, app: App, item: MessagesSearch, position: Int, adapter: MessagesAdapter) {
        b.searchEdit.removeTextChangedListener(adapter.textWatcher)
        b.searchEdit.addTextChangedListener(adapter.textWatcher)

        /*b.searchEdit.setOnKeyboardListener(object : TextInputKeyboardEdit.KeyboardListener {
            override fun onStateChanged(keyboardEditText: TextInputKeyboardEdit, showing: Boolean) {
                item.isFocused = showing
            }
        })*/

        /*if (b.searchEdit.text.toString() != item.searchText) {
            b.searchEdit.setText(item.searchText)
            b.searchEdit.setSelection(item.searchText.length)
        }*/

        //b.searchLayout.helperText = app.getString(R.string.messages_search_results, item.count)

        /*if (item.isFocused && !b.searchEdit.isFocused)
            b.searchEdit.requestFocus()*/
    }
}

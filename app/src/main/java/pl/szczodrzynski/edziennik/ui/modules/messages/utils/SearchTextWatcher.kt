/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-14.
 */

package pl.szczodrzynski.edziennik.ui.modules.messages.utils

import android.text.Editable
import android.text.TextWatcher
import pl.szczodrzynski.edziennik.R
import pl.szczodrzynski.edziennik.databinding.MessagesListItemSearchBinding
import pl.szczodrzynski.edziennik.ui.modules.messages.models.MessagesSearch

class SearchTextWatcher(
    private val b: MessagesListItemSearchBinding,
    private val filter: MessagesFilter,
    private val item: MessagesSearch
) : TextWatcher {

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

    override fun afterTextChanged(s: Editable?) {
        item.searchText = s ?: ""
        filter.filter(s) { count ->
            if (s.isNullOrBlank())
                b.searchLayout.helperText = " "
            else
                b.searchLayout.helperText =
                    b.root.context.getString(R.string.messages_search_results, count - 1)
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is SearchTextWatcher
    }

    override fun hashCode(): Int {
        var result = b.hashCode()
        result = 31 * result + filter.hashCode()
        result = 31 * result + item.hashCode()
        return result
    }
}

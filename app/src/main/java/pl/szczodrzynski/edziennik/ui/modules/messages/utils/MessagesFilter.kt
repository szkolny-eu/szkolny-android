/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-14.
 */

package pl.szczodrzynski.edziennik.ui.modules.messages.utils

import android.widget.Filter
import pl.szczodrzynski.edziennik.cleanDiacritics
import pl.szczodrzynski.edziennik.data.db.entity.Message
import pl.szczodrzynski.edziennik.data.db.full.MessageFull
import pl.szczodrzynski.edziennik.ui.modules.messages.MessagesAdapter
import java.util.*
import kotlin.math.min

class MessagesFilter(
    private val adapter: MessagesAdapter
) : Filter() {
    companion object {
        private const val NO_MATCH = 1000
    }

    private val comparator = MessagesComparator()
    private var prevCount = -1

    private val allItems
        get() = adapter.allItems

    private fun getMatchWeight(name: CharSequence?, prefix: CharSequence): Int {
        if (name == null)
            return NO_MATCH

        val prefixClean = prefix.cleanDiacritics()
        val nameClean = name.cleanDiacritics()

        return when {
            // First match against the whole, non-split value
            nameClean.startsWith(prefixClean, ignoreCase = true) -> 1
            // check if prefix matches any of the words
            nameClean.split(" ").any {
                it.startsWith(prefixClean, ignoreCase = true)
            } -> 2
            // finally check if the prefix matches any part of the name
            nameClean.contains(prefixClean, ignoreCase = true) -> 3

            else -> NO_MATCH
        }
    }

    override fun performFiltering(prefix: CharSequence?): FilterResults {
        val results = FilterResults()

        if (prevCount == -1)
            prevCount = allItems.size

        if (prefix.isNullOrBlank()) {
            allItems.forEach {
                if (it is MessageFull)
                    it.searchHighlightText = null
            }
            results.values = allItems.toList()
            results.count = allItems.size
            return results
        }

        val items = mutableListOf<Any>()

        allItems.forEach {
            if (it !is MessageFull) {
                items.add(it)
                return@forEach
            }
            it.filterWeight = NO_MATCH
            it.searchHighlightText = null

            var weight: Int
            // weights 11..13 and 110
            if (it.type == Message.TYPE_SENT) {
                it.recipients?.forEach { recipient ->
                    weight = getMatchWeight(recipient.fullName, prefix)
                    if (weight != NO_MATCH) {
                        if (weight == 3)
                            weight = 100
                        it.filterWeight = min(it.filterWeight, 10 + weight)
                    }
                }
            } else {
                weight = getMatchWeight(it.senderName, prefix)
                if (weight != NO_MATCH) {
                    if (weight == 3)
                        weight = 100
                    it.filterWeight = min(it.filterWeight, 10 + weight)
                }
            }

            // weights 21..23 and 120
            weight = getMatchWeight(it.subject, prefix)
            if (weight != NO_MATCH) {
                if (weight == 3)
                    weight = 100
                it.filterWeight = min(it.filterWeight, 20 + weight)
            }

            // weights 31..33 and 130
            weight = getMatchWeight(it.body, prefix)
            if (weight != NO_MATCH) {
                if (weight == 3)
                    weight = 100
                it.filterWeight = min(it.filterWeight, 30 + weight)
            }

            if (it.filterWeight != NO_MATCH) {
                it.searchHighlightText = prefix
                items.add(it)
            }
        }

        Collections.sort(items, comparator)
        results.values = items
        results.count = items.size
        return results
    }

    override fun publishResults(constraint: CharSequence?, results: FilterResults) {
        results.values?.let {
            adapter.items = it as MutableList<Any>
        }
        // do not re-bind the search box
        val count = results.count - 1

        // this tries to update every item except the search field
        with(adapter) {
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
        }

        prevCount = count
    }
}

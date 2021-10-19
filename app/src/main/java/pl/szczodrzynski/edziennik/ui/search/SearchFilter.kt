/*
 * Copyright (c) Kuba Szczodrzyński 2021-4-14.
 */

package pl.szczodrzynski.edziennik.ui.search

import android.widget.Filter
import pl.szczodrzynski.edziennik.ext.cleanDiacritics
import java.util.*
import kotlin.math.min

class SearchFilter<T : Searchable<T>>(
    private val adapter: SearchableAdapter<T>,
) : Filter() {
    companion object {
        private const val NO_MATCH = 1000
    }

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
                it.searchPriority = NO_MATCH
                it.searchHighlightText = null
            }
            results.values = allItems.toList()
            results.count = allItems.size
            return results
        }

        val newItems = allItems.mapNotNull { item ->
            if (item is SearchField) {
                return@mapNotNull item
            }
            item.searchPriority = NO_MATCH
            item.searchHighlightText = null

            // get all keyword sets from the entity
            val searchKeywords = item.searchKeywords
            // a temporary variable for the loops below
            var matchWeight: Int

            searchKeywords.forEachIndexed { priority, keywords ->
                keywords ?: return@forEachIndexed
                keywords.forEach { keyword ->
                    matchWeight = getMatchWeight(keyword, prefix)
                    if (matchWeight != NO_MATCH) {
                        // a match not at the word start boundary should be least prioritized
                        if (matchWeight == 3)
                            matchWeight = 100
                        item.searchPriority = min(item.searchPriority, priority * 10 + matchWeight)
                    }
                }
            }

            if (item.searchPriority != NO_MATCH) {
                // the adapter is reversed, the search priority also should be
                if (adapter.isReversed)
                    item.searchPriority *= -1
                item.searchHighlightText = prefix.toString()
                return@mapNotNull item
            }
            return@mapNotNull null
        }

        results.values = newItems.sorted()
        results.count = newItems.size
        return results
    }

    override fun publishResults(constraint: CharSequence?, results: FilterResults) {
        results.values?.let {
            @Suppress("UNCHECKED_CAST") // yes I know it's checked.
            adapter.setFilteredItems(it as List<T>)
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

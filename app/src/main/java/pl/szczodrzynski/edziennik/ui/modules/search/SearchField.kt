/*
 * Copyright (c) Kuba Szczodrzyński 2020-4-5.
 */

package pl.szczodrzynski.edziennik.ui.modules.search

class SearchField(
    var searchText: CharSequence = "",
) : Searchable<SearchField> {

    override val searchKeywords = emptyList<List<String>>()
    override var searchPriority = 0
    override var searchHighlightText: CharSequence? = null
    override fun compareTo(other: Searchable<*>) = 0
}
